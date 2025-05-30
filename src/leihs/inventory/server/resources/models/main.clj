(ns leihs.inventory.server.resources.models.main
  (:require
   [clojure.set]
   [clojure.string :as str]
   [honey.sql :refer [format]
    :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.models.form.license.common :refer [remove-nil-entries-fnc remove-nil-entries]]
   [leihs.inventory.server.resources.models.form.model.common :refer [create-image-url]]
   [leihs.inventory.server.resources.models.helper :refer [str-to-bool]]
   [leihs.inventory.server.resources.models.models-by-pool :refer [apply-is_deleted-context-if-valid
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

(defn extract-manufacturers [data]
  (mapv :manufacturer data))

(defn get-manufacturer-handler [request]
  (try
    (let [tx (:tx request)
          query-params (query-params request)
          mtype (:type query-params)
          search-term (:search-term query-params)
          in-detail (str-to-bool (:in-detail query-params))
          select-stm (if in-detail
                       (sql/select-distinct :m.id :m.manufacturer :m.product :m.version [:m.id :model_id])
                       (sql/select-distinct :m.manufacturer))

          base-query (-> select-stm
                         (sql/from [:models :m])
                         (sql/where [:is-not-null :m.manufacturer])
                         (sql/where [:not-like :m.manufacturer " %"])
                         (sql/where [:not-in :m.manufacturer [""]])
                         (sql/order-by [:m.manufacturer :asc])
                         (cond-> (not (str/blank? search-term))
                           (sql/where [:or [:ilike :m.manufacturer (str "%" search-term "%")]
                                       [:ilike :m.product (str "%" search-term "%")]]))
                         (cond-> (some? mtype)
                           (sql/where [:= :m.type mtype])))

          result (jdbc/execute! tx (-> base-query sql-format))]

      (response (if in-detail result (extract-manufacturers result))))
    (catch Exception e
      (error "Failed to get models/manufacturer" e)
      (bad-request {:error "Failed to get models/manufacture" :details (.getMessage e)}))))

(defn get-models-compatible-handler [request]
  (try
    (let [tx (:tx request)
          model_id (-> request path-params :model_id)
          base-query (-> (sql/select [:models.id :model_id]
                                     :models.product
                                     :models.version
                                     (create-image-url :models :cover_image_url)
                                     :models.cover_image_id)
                         (sql/from :models)
                         (sql/left-join :images [:= :models.cover_image_id :images.id])
                         (cond-> model_id
                           (-> (sql/join :models_compatibles [:= :models_compatibles.model_id model_id])
                               (sql/where [:= :models.id model_id])))
                         (sql/order-by [[:trim [:|| :models.product " " :models.version]] :asc]))
          {:keys [page size]} (fetch-pagination-params-raw request)]
      (if (or model_id (and (nil? page) (nil? size)))
        (-> (jdbc/execute! tx (-> base-query sql-format))
            remove-nil-entries-fnc
            response)
        (response (create-paginated-response base-query tx size page remove-nil-entries-fnc))))
    (catch Exception e
      (error "Failed to get models-compatible" e)
      (bad-request {:error "Failed to get models-compatible" :details (.getMessage e)}))))

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

(defn create-model-handler [request]
  (let [created_ts (LocalDateTime/now)
        body-params (:body-params request)
        tx (:tx request)
        model (assoc body-params
                     :created_at created_ts
                     :updated_at created_ts)]
    (try
      (let [res (jdbc/execute! tx (-> (sql/insert-into :models)
                                      (sql/values [model])
                                      (sql/returning :*)
                                      sql-format))
            model-id (:id res)]
        (if res
          (response res)
          (bad-request {:error "Failed to create model"})))
      (catch Exception e
        (error "Failed to create model" e)
        (bad-request {:error "Failed to create model" :details (.getMessage e)})))))

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
