(ns leihs.inventory.server.resources.pool.groups.group.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [path-params]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]]))

(defn get-groups-of-pool-handler [request]
  (try
    (let [tx (:tx request)
          pool_id (-> request path-params :pool_id)
          group_id (-> request path-params :group_id)
          query (-> (sql/select :g.*)
                    (sql/from [:groups :g])
                    (sql/join [:group_access_rights :gar] [:= :g.id :gar.group_id])
                    (sql/where [:= :gar.inventory_pool_id pool_id])
                    (cond-> group_id (sql/where [:= :g.id group_id]))
                    (sql/limit 50)
                    sql-format)
          result (jdbc/query tx query)]
      (response result))
    (catch Exception e
      (error "Failed to get group" e)
      (bad-request {:error "Failed to get group" :details (.getMessage e)}))))
