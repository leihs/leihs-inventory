(ns leihs.inventory.server.resources.models.models-by-pool
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
  (:import (java.time LocalDateTime)))

(defn get-pools-handler [request]
  (let [tx (:tx request)
        pool_id (get-in request [:path-params :pool_id])
        model_id (get-in request [:path-params :model_id])
        models-query (->
                       ;(sql/with-recursive
                       ;  :model_group_links
                       ;  (sql/union
                       ;    ;; Anchor member
                       ;    (sql/select :mg.id :mg.name)
                       ;    (sql/from [:model_groups :mg])
                       ;    (sql/where [:in :mg.id
                       ;                (sql/select :pmg.model_group_id)
                       ;                (sql/from :inventory_pools_model_groups :pmg)
                       ;                (sql/where [:= :pmg.inventory_pool_id [:cast pool_id :uuid]])])
                       ;    ;; Recursive member
                       ;    (sql/select :child_mg.id :child_mg.name)
                       ;    (sql/from [:model_group_links :parent_mg])
                       ;    (sql/join [:model_group_links :mgl] [:= :parent_mg.id :mgl.parent_id])
                       ;    (sql/join [:model_groups :child_mg] [:= :mgl.child_id :child_mg.id])))

                      (sql/select :p.id [:m.id :model_id] [:p.name :pool_name] [:m.product])
                      (sql/from [:inventory_pools :p])
                      (sql/join [:inventory_pools_model_groups :pmg] [:= :p.id :pmg.inventory_pool_id])
                      (sql/join [:model_links :ml] [:= :pmg.model_group_id :ml.model_group_id])
                      (sql/join [:models :m] [:= :ml.model_id :m.id])
                      (sql/where [:= :p.id [:cast pool_id :uuid]])
                      (cond-> model_id (sql/where [:= :m.id [:cast model_id :uuid]])))
        res (-> models-query
                sql-format
                (->> (jdbc/query tx)))]
    (response res)))

(defn get-models-handler
  ([request]
   (get-models-handler request false))
  ([request with-pagination?]
   (let [tx (:tx request)
         {:keys [pool_id model_id]} (path-params request)
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
         base-query (-> (sql/select-distinct :m.*)
                        (sql/from [:inventory_pools :p])
                        (sql/join [:inventory_pools_model_groups :pmg] [:= :p.id :pmg.inventory_pool_id])
                        (sql/join [:model_links :ml] [:= :pmg.model_group_id :ml.model_group_id])
                        (sql/join [:models :m] [:= :ml.model_id :m.id])
                        (sql/where [:= :p.id [:cast pool_id :uuid]])
                        (cond-> filter-manufacturer
                          (sql/where [:ilike :m.manufacturer (str "%" filter-manufacturer "%")]))
                        (cond-> filter-product
                          (sql/where [:ilike :m.product (str "%" filter-product "%")]))
                        (cond-> model_id (sql/where [:= :m.id model_id]))
                        (cond-> model_id (sql/order-by sort-by)))]
     (cond
       (and (not model_id) with-pagination?)
       (let [{:keys [page size]} (fetch-pagination-params request)]
         (create-paginated-response base-query tx size page))
       :else (jdbc/query tx (-> base-query sql-format))))))

(defn get-models-of-pool-with-pagination-handler [request]
  (get-models-handler request true))

(defn get-models-of-pool-handler [request]
  (let [result (get-models-handler request)]
    (response result)))

(defn create-model-handler-by-pool [request]
  (let [created_ts (LocalDateTime/now)
        body-params (:body-params request)
        tx (:tx request)
        model (assoc body-params
                     :created_at created_ts
                     :updated_at created_ts)]
    (try
      (if-let [res (jdbc/insert! tx :models model)]
        (response res)
        (bad-request {:error "Failed to create model"}))
      (catch Exception e
        (error "Failed to create model" e)
        (bad-request {:error "Failed to create model" :details (.getMessage e)})))))

(defn update-model-handler-by-pool [request]
  (let [model-id (get-in request [:path-params :id])
        body-params (:body-params request)
        tx (:tx request)
        model body-params
        available-models (first (get-models-handler request))
        model-id (:id available-models)]
    (try
      (let [res (jdbc/update! tx :models model ["id = ?::uuid" model-id])]
        (if (= 1 (:next.jdbc/update-count res))
          (response {:message "Model updated" :id model-id})
          (bad-request {:error "Failed to update model" :details "Model not found"})))
      (catch Exception e
        (error "Failed to update model" e)
        (bad-request {:error "Failed to update model" :details (.getMessage e)})))))

(defn delete-model-handler-by-pool [request]
  (let [tx (:tx request)
        model-id (get-in request [:path-params :id])
        model-id (:id (first (get-models-handler request)))]
    (try
      (let [res (jdbc/delete! tx :models ["id = ?::uuid" model-id])]
        (if (= 1 (:next.jdbc/update-count res))
          (response {:message "Model deleted" :id model-id})
          (bad-request {:error "Failed to delete model" :details "Model not found"})))
      (catch Exception e
        (error "Failed to delete model" e)
        (status
         (bad-request
          {:error "Failed to delete model"
           :details (.getMessage e)})
         409)))))
