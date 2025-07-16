(ns leihs.inventory.server.resources.pool.rooms.room.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.utils.request-utils :refer [path-params
                                                       query-params]]
   [next.jdbc :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request header response]]
   [taoensso.timbre :refer [error]]))

(defn get-resource [request]
  (try
    (let [tx (:tx request)
          building-id (-> request query-params :building_id)
          rooms-id (-> request path-params :rooms_id)
          query (-> (sql/select :r.*)
                    (sql/from [:rooms :r])
                    (cond-> rooms-id (sql/where [:= :r.id rooms-id]))
                    (cond-> building-id (sql/where [:= :r.building_id building-id]))
                    sql-format)
          result (jdbc/execute-one! tx query)]
      (-> (response result)
          (header "Count" (count result))))
    (catch Exception e
      (error "Failed to get rooms" e)
      (bad-request {:error "Failed to get rooms" :details (.getMessage e)}))))
