(ns leihs.inventory.server.resources.pool.models.model.accessories.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [path-params]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]]))

(defn get-accessories-handler [request]
  (try
    (let [tx (:tx request)
          accessories_id (-> request path-params :id)
          query (-> (sql/select :a.*)
                  (sql/from [:accessories :a])
                  (cond-> accessories_id (sql/where [:= :a.id accessories_id]))
                  (sql/limit 10)
                  sql-format)
          result (jdbc/query tx query)]
      (response result))
    (catch Exception e
      (error "Failed to get accessories" e)
      (bad-request {:error "Failed to get accessories" :details (.getMessage e)}))))

(defn get-accessories-of-pool-handler [request]
  (try
    (let [tx (:tx request)
          pool_id (-> request path-params :pool_id)
          model_id (-> request path-params :model_id)
          accessories_id (-> request path-params :accessory_id)
          query (-> (sql/select :a.*)
                  (sql/from [:accessories :a])
                  (sql/join [:accessories_inventory_pools :aip]
                    [:= :aip.accessory_id :a.id])
                  (sql/where [:= :aip.inventory_pool_id pool_id] [:= :a.model_id model_id])
                  (cond-> accessories_id (sql/where [:= :a.id accessories_id]))
                  sql-format)
          result (jdbc/query tx query)]
      ;; TODO: add pagination if size OR page is set: (create-pagination-response request base-query with-pagination?))))
      (response result))
    (catch Exception e
      (error "Failed to get accessories of pool" e)
      (bad-request {:error "Failed to get accessories" :details (.getMessage e)}))))