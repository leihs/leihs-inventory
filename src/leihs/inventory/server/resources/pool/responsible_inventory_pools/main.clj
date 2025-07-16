(ns leihs.inventory.server.resources.pool.responsible-inventory-pools.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.utils.request-utils :refer [query-params]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [debug error spy]]))

(defn get-resources [request]
  (let [tx (:tx request)
        pool-id (-> request :parameters :path :pool_id)
        res (-> (sql/select :inventory_pools.id,
                            :inventory_pools.name,
                            :inventory_pools.shortname)
                (sql/from :inventory_pools)
                (sql/where [:exists
                            (-> (sql/select 1)
                                (sql/from :items)
                                (sql/where [:= :items.inventory_pool_id :inventory_pools.id])
                                (sql/where [:= :items.owner_id pool-id]))])
                (sql/order-by :inventory_pools.name)
                sql-format
                (->> (jdbc/query tx)))]
    (response res)))
