(ns leihs.inventory.server.resources.pool.export.excel.routes
  (:require
   [leihs.inventory.server.constants :refer [HIDE_BASIC_ENDPOINTS]]
   [leihs.inventory.server.resources.pool.export.excel.main :refer [index-resources]]
   [reitit.coercion.schema]))

(defn get-export-excel-routes []
  ["/export"

   {:swagger {:tags [""]}
    :no-doc HIDE_BASIC_ENDPOINTS}

   ["/excel" {:get {:summary "Export Excel ( test-dummy )"
                    :accept "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    :coercion reitit.coercion.schema/coercion
                    :swagger {:produces ["application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"]}
                    :handler index-resources}}]])
