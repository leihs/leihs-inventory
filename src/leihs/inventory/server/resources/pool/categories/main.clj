(ns leihs.inventory.server.resources.pool.categories.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [path-params]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]]))

(defn get-model-groups-of-pool-handler [request]
  (try
    (let [tx (:tx request)
          pool_id (-> request path-params :pool_id)
          group_id (-> request path-params :model_group_id)
          type "Category"
          query (-> (sql/select :mg.*)
                    (sql/from [:model_groups :mg])
                    (sql/where [:ilike :mg.type (str type)])
                    (sql/order-by :mg.name)
                    (cond-> group_id (sql/where [:= :mg.id group_id]))
                    sql-format)
          result (jdbc/query tx query)]
      (response result))
    (catch Exception e
      (error "Failed to get items" e)
      (bad-request {:error "Failed to get items" :details (.getMessage e)}))))


