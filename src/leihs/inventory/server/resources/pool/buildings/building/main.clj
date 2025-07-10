(ns leihs.inventory.server.resources.pool.buildings.building.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [next.jdbc :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status header]]
   [taoensso.timbre :refer [error]]))

(defn get-resource [request]
  (try
    (let [tx (:tx request)
          building-id (-> request path-params :building_id)
          query (-> (sql/select :b.*)
                    (sql/from [:buildings :b])
                    (cond-> building-id (sql/where [:= :b.id building-id]))
                    sql-format)

          result (jdbc/execute-one! tx query)]
      (-> (response result)
          (header "Count" (count result))))
    (catch Exception e
      (error "Failed to get rooms" e)
      (bad-request {:error "Failed to get rooms" :details (.getMessage e)}))))


