(ns leihs.inventory.server.resources.pool.buildings.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.utils.request-utils :refer [path-params]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request header response]]
   [taoensso.timbre :refer [debug error]]))

(defn index-resources [request]
  (try
    (let [tx (:tx request)
          building-id (-> request path-params :building_id)
          query (-> (sql/select :b.*)
                    (sql/from [:buildings :b])
                    (cond-> building-id (sql/where [:= :b.id building-id]))
                    sql-format)

          result (jdbc/query tx query)]
      (-> (response result)
          (header "Count" (count result))))
    (catch Exception e
      (debug e)
      (error "Failed to get rooms" e)
      (bad-request {:error "Failed to get rooms" :details (.getMessage e)}))))
