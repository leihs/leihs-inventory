(ns leihs.inventory.server.resources.models.by-pool
  (:require
   [clojure.set]
   [honey.sql :refer [format]
    :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]])
  (:import (java.time LocalDateTime)))

(defn- get-available-models-of-pool [request]
  (let [tx (:tx request)
        pool_id (get-in request [:path-params :pool_id])
        model_id (get-in request [:path-params :model_id])
        ;; TODO: fix hierarchical model query
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

                      (sql/select :p.id
                                  ;[:mg_h.id :model_group_id]
                                  [:m.id :model_id]
                                  [:p.name :pool_name]
                                  ;[:mg_h.name :model_group_name]
                                  [:m.product])
                      (sql/from [:inventory_pools :p])
                      (sql/join [:inventory_pools_model_groups :pmg] [:= :p.id :pmg.inventory_pool_id])
                      ;(sql/join [:model_group_links :mg_h] [:= :pmg.model_group_id :mg_h.id])
                      (sql/join [:model_links :ml] [:= :pmg.model_group_id :ml.model_group_id])
                      (sql/join [:models :m] [:= :ml.model_id :m.id])
                      (sql/where [:= :p.id [:cast pool_id :uuid]])
                      (cond-> model_id (sql/where [:= :m.id [:cast model_id :uuid]])))]
    (-> models-query
        sql-format
        (->> (jdbc/query tx)))))

(defn get-models-of-pool-handler [request]
  (let [result (get-available-models-of-pool request)]
    {:body result}))

(defn create-model-handler-by-pool [request]
  (let [created_ts (LocalDateTime/now)
        body-params (:body-params request)
        tx (:tx request)
        model body-params
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

(defn update-model-handler-by-pool [request]
  (let [model-id (get-in request [:path-params :id])
        body-params (:body-params request)
        tx (:tx request)
        model body-params

        available-models (get-available-models-of-pool request)

        model-id (:model_id (first available-models))]

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
        available-models (get-available-models-of-pool request)
        model-id (:model_id (first available-models))]

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
