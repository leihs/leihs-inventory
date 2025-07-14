(ns leihs.inventory.server.resources.pool.export.csv.routes
  (:require
   [leihs.inventory.server.constants :refer [HIDE_BASIC_ENDPOINTS]]
   [leihs.inventory.server.resources.pool.export.csv.main :as csv]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [reitit.coercion.schema]))

(defn get-export-csv-routes []
  ["/export"

   {:swagger {:tags [""]}
    :no-doc HIDE_BASIC_ENDPOINTS}

   ["/csv" {:get {:summary "Export CSV ( test-dummy )"
                  :accept "text/csv"
                  :coercion reitit.coercion.schema/coercion
                  :swagger {:produces ["text/csv"]}
                  :handler csv/index-resources}}]])
