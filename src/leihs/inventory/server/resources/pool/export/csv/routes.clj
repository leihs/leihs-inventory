(ns leihs.inventory.server.resources.pool.export.csv.routes
  (:require
   [leihs.inventory.server.constants :refer [HIDE_BASIC_ENDPOINTS]]
   [leihs.inventory.server.resources.pool.export.csv.main :as export-csv]
   [reitit.coercion.schema]))

(defn routes []
  [""

   {:no-doc HIDE_BASIC_ENDPOINTS}

   ["/export/csv" {:get {:summary "Export CSV ( test-dummy )"
                         :accept "text/csv"
                         :coercion reitit.coercion.schema/coercion
                         :swagger {:produces ["text/csv"]}
                         :handler export-csv/index-resources}}]])
