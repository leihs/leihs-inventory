(ns leihs.inventory.server.resources.pools.routes
  (:require
   [clojure.set]
   [leihs.core.auth.session :refer [wrap-authenticate]]
   [leihs.inventory.server.resources.pools.main :refer [get-pools-handler]]
   [leihs.inventory.server.resources.utils.flag :refer [session admin]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware wrap-is-admin!]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn get-pools-routes []
  ["/"
   {:swagger {:conflicting true
              :tags ["Pool"] :security []}}

   ["user-pools-info"
    {:get {:conflicting true
           :summary (-> "(DEV)" session)
           :accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :middleware [wrap-is-admin! accept-json-middleware]
           :swagger {:produces ["application/json"]}
           :parameters {:query {:login s/Str}}
           :handler get-pools-handler
           :responses {200 {:description "OK"
                            :body s/Any}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]])
