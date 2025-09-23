(ns leihs.inventory.server.resources.pool.buildings.building.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.helper :refer [log-by-severity]]
   [leihs.inventory.server.utils.request-utils :refer [path-params]]
   [next.jdbc :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [response status]]))

(def ERROR_GET_BUILDING "Failed to get building")

(defn get-resource [request]
  (try
    (let [tx (:tx request)
          building-id (-> request path-params :building_id)
          query (-> (sql/select :b.*)
                    (sql/from [:buildings :b])
                    (cond-> building-id (sql/where [:= :b.id building-id]))
                    sql-format)
          result (jdbc/execute-one! tx query)]
      (if result
        (response result)
        (-> (response {:message "Building not found"})
            (status 404))))
    (catch Exception e
      (log-by-severity ERROR_GET_BUILDING e)
      (exception-handler request ERROR_GET_BUILDING e))))
