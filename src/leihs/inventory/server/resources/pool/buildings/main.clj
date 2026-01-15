(ns leihs.inventory.server.resources.pool.buildings.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.middlewares.debug :refer [log-by-severity]]
   [leihs.inventory.server.middlewares.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.request :refer [path-params]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [header response]]))

(def ERROR_GET_BUILDINGS "Failed to get buildings")

(def base-query
  (-> (sql/select :buildings.*)
      (sql/from :buildings)
      (sql/order-by :buildings.name)))

(defn get-by-room-id [tx room-id]
  (-> base-query
      (sql/join :rooms [:= :rooms.building_id :buildings.id])
      (sql/where [:= :rooms.id room-id])
      sql-format
      (->> (jdbc/query tx))
      first))

(defn index-resources [request]
  (try
    (let [tx (:tx request)
          building-id (-> request path-params :building_id)
          query (-> base-query
                    (cond-> building-id (sql/where [:= :b.id building-id]))
                    sql-format)

          result (jdbc/query tx query)]
      (-> (response result)
          (header "Count" (count result))))
    (catch Exception e
      (log-by-severity ERROR_GET_BUILDINGS e)
      (exception-handler request ERROR_GET_BUILDINGS e))))
