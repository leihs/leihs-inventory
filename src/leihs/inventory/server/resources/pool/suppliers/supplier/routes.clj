(ns leihs.inventory.server.resources.pool.suppliers.supplier.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.auth.session :as session]
   [leihs.inventory.server.resources.pool.suppliers.supplier.main :refer [get-suppliers-auto-pagination-handler]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.coercion.core :refer [pagination]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(def resp-supplier [{:id s/Uuid
                     :name s/Str
                     :note (s/maybe s/Str)}])

(defn get-suppliers-single-routes []
  [""
   ["/supplier"
    {:swagger {:conflicting true
               :tags []}}

    ["/:supplier_id"
     {:get {:conflicting true
            :summary "OK | Lieferant anzeigen [fe]"
            :accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :middleware [accept-json-middleware session/wrap]

            :swagger {:produces ["application/json"]}
            :parameters {:path {:supplier_id s/Uuid}}
            :handler get-suppliers-auto-pagination-handler
            :responses {200 {:description "OK"
                             :body resp-supplier}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]]])
