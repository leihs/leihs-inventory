(ns leihs.inventory.server.resources.models.main
  (:require
   [clojure.set]
   [honey.sql :refer [format]
    :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.pagination :refer [create-paginated-response fetch-pagination-params]]
   [next.jdbc.sql :as jdbc]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]])
  (:import [java.net URL JarURLConnection]
           (java.time LocalDateTime)
           [java.util.jar JarFile]))

(defn get-models-compatible-handler [request]
  (try
    (let [tx (:tx request)
          model_id (-> request path-params :model_id)
          base-query (-> (sql/select [:m.id :model_id] :m2.*)
                         (sql/from [:models_compatibles :mc])
                         (sql/join [:models :m] [:= :mc.model_id :m.id])
                         (sql/join [:models :m2] [:= :mc.compatible_id :m2.id])
                         (cond-> model_id (sql/where [:= :m.id model_id])))]
      (if model_id
        (response (jdbc/query tx (-> base-query sql-format)))
        (let [{:keys [page size]} (fetch-pagination-params request)]
          (create-paginated-response base-query tx size page))))
    (catch Exception e
      (error "Failed to get user" e)
      (bad-request {:error "Failed to get user" :details (.getMessage e)}))))

(defn get-models-handler [request]
  (let [tx (:tx request)
        model_id (-> request path-params :id)
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
        base-query (-> (sql/select :*)
                       (sql/from [:models :m])
                       (cond-> filter-manufacturer
                         (sql/where [:ilike :m.manufacturer (str "%" filter-manufacturer "%")]))
                       (cond-> filter-product
                         (sql/where [:ilike :m.product (str "%" filter-product "%")]))
                       (cond-> model_id (sql/where [:= :m.id model_id]))
                       (sql/order-by sort-by))]
    (if model_id
      (response (jdbc/query tx (-> base-query sql-format)))
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
      (let [res (jdbc/insert! tx :models model)]
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
      (let [res (jdbc/update! tx :models model ["id = ?::uuid" model-id])]
        (if (= 1 (:next.jdbc/update-count res))
          (response {:message "Model updated" :id model-id})
          (bad-request {:error "Failed to update model" :details "Model not found"})))
      (catch Exception e
        (error "Failed to update model" e)
        (bad-request {:error "Failed to update model" :details (.getMessage e)})))))

(defn delete-model-handler [request]
  (let [tx (:tx request)
        model-id (get-in request [:path-params :model_id])]
    (try
      (let [res (jdbc/delete! tx :models ["id = ?::uuid" model-id])]
        (if (= 1 (:next.jdbc/update-count res))
          (response {:message "Model deleted" :id model-id})
          (bad-request {:error "Failed to delete model" :details "Model not found"})))
      (catch Exception e
        (error "Failed to delete model" e)
        (status (bad-request {:error "Failed to delete model"
                              :details (.getMessage e)}) 409)))))
