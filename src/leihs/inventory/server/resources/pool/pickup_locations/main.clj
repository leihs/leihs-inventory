(ns leihs.inventory.server.resources.pool.pickup-locations.main
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.utils.transform :refer [to-uuid]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [response]]))

(defn pool-has-pickup-locations?
  [tx pool-id]
  (boolean
   (jdbc/execute-one!
    tx
    (-> (sql/select 1)
        (sql/from :pickup_locations)
        (sql/where [:= :inventory_pool_id pool-id])
        (sql/limit 1)
        sql-format))))

(defn index-resources [request]
  (let [tx (:tx request)
        pool-id (to-uuid (get-in request [:path-params :pool_id]))]
    (response {:has_pickup_locations (pool-has-pickup-locations? tx pool-id)})))
