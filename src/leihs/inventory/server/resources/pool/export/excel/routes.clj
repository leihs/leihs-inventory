(ns leihs.inventory.server.resources.pool.export.excel.routes
  (:require
   [leihs.inventory.server.constants :refer [HIDE_BASIC_ENDPOINTS]]
   [leihs.inventory.server.resources.pool.export.excel.main :as export-excel]
   [reitit.coercion.schema]))

(defn routes []
  [""

   {:no-doc HIDE_BASIC_ENDPOINTS}

   ["/export/excel" {:get {:summary "Export Excel ( test-dummy )"
                           :accept "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                           :coercion reitit.coercion.schema/coercion
                           :swagger {:produces ["application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"]}
                           :handler export-excel/index-resources}}]])
