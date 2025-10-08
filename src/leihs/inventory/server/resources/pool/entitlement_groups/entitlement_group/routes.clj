(ns leihs.inventory.server.resources.pool.entitlement-groups.entitlement-group.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.entitlement-groups.entitlement-group.main :as entitlement-group]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  ["/entitlement-groups/:entitlement_group_id"
   {:get {:summary (fe "a.k.a 'Anspruchsgruppe'")
          :accept "application/json"
          :coercion reitit.coercion.schema/coercion
          :parameters {:path {
                              :pool_id s/Uuid
                              :entitlement_group_id s/Uuid
                              }
                       }
          :produces ["application/json"]
          :handler entitlement-group/get-resource
          :responses {200 {:description "OK"
                           ;:body [response-body]
                           }
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}




    }])
