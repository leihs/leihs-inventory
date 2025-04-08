(ns leihs.inventory.server.resources.export.routes
  (:require
   [leihs.inventory.server.resources.export.main :refer [csv-handler excel-handler]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [reitit.coercion.schema]))

(defn get-export-routes []
  ["/export"

   {:swagger {:conflicting true
              :tags ["Export"]}}

   ["/csv" {:get {:summary "Export CSV ( test-dummy )"
                  :accept "text/csv"
                  :coercion reitit.coercion.schema/coercion
                  :swagger {:produces ["text/csv"]}
                  :handler csv-handler}}]

   ["/excel" {:get {:summary "Export Excel ( test-dummy )"
                    :accept "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    :coercion reitit.coercion.schema/coercion
                    :swagger {:produces ["application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"]}
                    :handler excel-handler}}]])
