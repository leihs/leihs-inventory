(ns leihs.inventory.server.resources.pool.inventory-pools.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [response]]))

(def base-query
  (-> (sql/select :inventory_pools.id,
                  :inventory_pools.name,
                  :inventory_pools.shortname)
      (sql/from :inventory_pools)
      (sql/where [:= :inventory_pools.is_active true])))

(defn get-by-id [tx id]
  (-> base-query
      (sql/where [:= :inventory_pools.id id])
      sql-format
      (->> (jdbc/query tx))
      first))

(defn index-resources [request]
  (let [tx (:tx request)
        pool-id (-> request :parameters :path :pool_id)
        responsible (-> request :parameters :query :responsible)
        res (-> base-query
                (cond-> responsible
                  (sql/where [:exists
                              (-> (sql/select 1)
                                  (sql/from :items)
                                  (sql/where [:= :items.inventory_pool_id :inventory_pools.id])
                                  (sql/where [:= :items.owner_id pool-id]))]))
                (sql/order-by :inventory_pools.name)
                sql-format
                (->> (jdbc/query tx)))]
    (response res)))
