(ns leihs.inventory.server.resources.pool.models.model.main
  (:require
   [clojure.set]
   [clojure.string :as str]
   [honey.sql :refer [format]
    :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.common :refer [str-to-bool  remove-nil-entries-fnc remove-nil-entries
                                                         apply-is_deleted-context-if-valid
                                                         apply-is_deleted-where-context-if-valid]]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.helper :refer [convert-map-if-exist url-ends-with-uuid?]]
   [leihs.inventory.server.utils.pagination :refer [create-paginated-response fetch-pagination-params fetch-pagination-params-raw]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [debug error spy]])
  (:import [java.net URL JarURLConnection]
           (java.time LocalDateTime)
           [java.util.jar JarFile]))

(defn get-models-handler [request]
  (let [tx (:tx request)
        model_id (-> request path-params :model_id)
        query-params (query-params request)
        {:keys [page size]} (fetch-pagination-params request)
        sort-by (case (:sort_by query-params)
                  :manufacturer-asc [:m.manufacturer :asc]
                  :manufacturer-desc [:m.manufacturer :desc]
                  :product-asc [:m.product :asc]
                  :product-desc [:m.product :desc]
                  [:m.product :asc])
        filter-manufacturer (if-not model_id (:filter_manufacturer query-params) nil)
        filter-product (if-not model_id (:filter_product query-params) nil)
        is_deletable (if-not model_id (:is_deletable query-params) nil)
        base-query (-> (apply-is_deleted-context-if-valid is_deletable)
                       (cond-> filter-manufacturer
                         (sql/where [:ilike :m.manufacturer (str "%" filter-manufacturer "%")]))
                       (cond-> filter-product
                         (sql/where [:ilike :m.product (str "%" filter-product "%")]))
                       (cond-> model_id (sql/where [:= :m.id model_id]))
                       (sql/order-by sort-by))
        base-query (apply-is_deleted-where-context-if-valid base-query is_deletable)]
    (if (url-ends-with-uuid? (:uri request))
      (let [res (jdbc/execute-one! tx (-> base-query sql-format))]
        (if res
          (response res)
          (status 404)))
      (let [{:keys [page size]} (fetch-pagination-params request)]
        (response (create-paginated-response base-query tx size page))))))


(defn update-model-handler [request]
  (let [model-id (get-in request [:path-params :model_id])
        body-params (:body-params request)
        tx (:tx request)
        model body-params]
    (try
      (let [res (jdbc/execute! tx (-> (sql/update :models)
                                      (sql/set (convert-map-if-exist body-params))
                                      (sql/where [:= :id (to-uuid model-id)])
                                      (sql/returning :*)
                                      sql-format))]
        (if (= 1 (count res))
          (response res)
          (bad-request {:error "Failed to update model" :details "Model not found"})))
      (catch Exception e
        (error "Failed to update model" e)
        (bad-request {:error "Failed to update model" :details (.getMessage e)})))))

(defn delete-model-handler [request]
  (let [tx (:tx request)
        model-id (get-in request [:path-params :model_id])]
    (try
      (let [res (jdbc/execute! tx (-> (sql/delete-from :models)
                                      (sql/where [:= :id (to-uuid model-id)])
                                      (sql/returning :*)
                                      sql-format))]
        (if (= 1 (count res))
          (response res)
          (bad-request {:error "Failed to delete model" :details "Model not found"})))
      (catch Exception e
        (error "Failed to delete model" e)
        (status (bad-request {:error "Failed to delete model"
                              :details (.getMessage e)}) 409)))))