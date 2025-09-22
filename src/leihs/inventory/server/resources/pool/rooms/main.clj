(ns leihs.inventory.server.resources.pool.rooms.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.helper :refer [log-by-severity]]
   [leihs.inventory.server.utils.request-utils :refer [path-params
                                                       query-params]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [header response]]))

(def ERROR_GET_ROOMS "Failed to get rooms")

(defn index-resources [request]
  (try
    (let [tx (:tx request)
          building-id (-> request query-params :building_id)
          rooms-id (-> request path-params :rooms_id)
          query (-> (sql/select :r.*)
                    (sql/from [:rooms :r])
                    (cond-> rooms-id (sql/where [:= :r.id rooms-id]))
                    (cond-> building-id (sql/where [:= :r.building_id building-id]))
                    sql-format)
          result (jdbc/query tx query)]
      (-> (response result)
          (header "Count" (count result))))
    (catch Exception e
      (log-by-severity ERROR_GET_ROOMS e)
      (exception-handler request ERROR_GET_ROOMS e))))
