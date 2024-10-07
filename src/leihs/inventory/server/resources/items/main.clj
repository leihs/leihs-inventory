(ns leihs.inventory.server.resources.items.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [path-params]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]]))

(defn get-items-of-pool-handler [request]
  (try
    (let [tx (:tx request)
          pool_id (-> request path-params :pool_id)
          item_id (-> request path-params :id)
          query (-> (sql/select :i.*)
                    (sql/from [:items :i])
                    (sql/where [:= :i.inventory_pool_id pool_id])
                    (cond-> item_id (sql/where [:= :i.id item_id]))
                    (sql/limit 10)
                    sql-format)
          result (jdbc/query tx query)]
      (response result))
    (catch Exception e
      (error "Failed to get items" e)
      (bad-request {:error "Failed to get items" :details (.getMessage e)}))))
