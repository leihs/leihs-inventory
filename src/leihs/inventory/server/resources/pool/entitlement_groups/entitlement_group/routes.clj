(ns leihs.inventory.server.resources.pool.entitlement-groups.entitlement-group.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.entitlement-groups.entitlement-group.main :as entitlement-group]
   ;[leihs.inventory.server.resources.pool.entitlement-groups.types :refer [response-body]]

   ;[leihs.inventory.server.resources.pool.options.types]

   ;[reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn create-description [url]
  (str "- GET " url " Accept: application/json "))

(defn routes []
  ["/entitlement-groups/:entitlement_group_id"
   {:get {:summary (fe "a.k.a 'Anspruchsgruppe'")
          ;:description (create-description "https://staging.leihs.zhdk.ch/manage/8bd16d45-056d-5590-bc7f-12849f034351/groups")
          :accept "application/json"
          :coercion reitit.coercion.schema/coercion
          ;:swagger {:produces ["application/json"]}
          ;
          ;
          ;:coercion reitit.coercion.schema/coercion
          ;:swagger {:produces ["application/json"]}
          ;:parameters {:path {:pool_id s/Uuid
          ;                    :building_id s/Uuid}}

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
                      500 {:description "Internal Server Error"}}}}])
