
(ns leihs.inventory.server.resources.buildings-rooms.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.resources.utils.request :refer [path-params]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status header]]
   [taoensso.timbre :refer [error]]))

(defn get-buildings-handler [request]
  (try
    (let [tx (:tx request)
          ;pool_id (-> request path-params :pool_id)
          ;model_link_id (-> request path-params :id)
          building-id (-> request path-params :building_id)
          query (-> (sql/select :b.*)
                    (sql/from [:buildings :b])
                  (cond-> building-id (sql/where [:= :b.id building-id]))
                    sql-format)

          result (jdbc/query tx query)]

      ;(response result)

      (-> (response result)
        (header "Count" (count result)))


      )
    (catch Exception e
      (error "Failed to get rooms" e)
      (bad-request {:error "Failed to get rooms" :details (.getMessage e)}))))

(defn get-rooms-handler [request]
  (try
    (let [tx (:tx request)
          ;pool_id (-> request path-params :pool_id)
          ;model_link_id (-> request path-params :id)

          ;search-term (-> request query-params :search-term)

          building-id (-> request query-params :building_id)
          p (println ">o> building-id" building-id)

          rooms-id (-> request path-params :rooms_id)
          query (-> (sql/select :r.*)
                    (sql/from [:rooms :r])
                  (cond-> rooms-id (sql/where [:= :r.id rooms-id]))
                  (cond-> building-id (sql/where [:= :r.building_id building-id]))
                    sql-format)

          result (jdbc/query tx query)]

      ;(response result)

      (-> (response result)
        (header "Count" (count result)))

      )
    (catch Exception e
      (error "Failed to get rooms" e)
      (bad-request {:error "Failed to get rooms" :details (.getMessage e)}))))