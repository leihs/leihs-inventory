(ns leihs.inventory.server.resources.entitlement-groups.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.entitlement-groups.main :refer [
                                                             get-entitlement-groups-of-pool-handler
                                                                     ;get-model-groups-of-pool-handler
                                                             ;get-groups-of-pool-handler
                                                             ;get-model-group-links-of-pool-handler
                                                             ]]
   ;[leihs.inventory.server.resources.models.main :refer [get-models-handler
   ;                                                      create-model-handler
   ;                                                      update-model-handler
   ;                                                      delete-model-handler]]
   ;[leihs.inventory.server.resources.models.models-by-pool :refer [get-models-of-pool-handler
   ;                                                                create-model-handler-by-pool
   ;                                                                get-models-of-pool-handler
   ;                                                                update-model-handler-by-pool
   ;                                                                delete-model-handler-by-pool]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn create-description [url]
  (str "- GET " url " Accept: application/json "))

(defn get-entitlement-groups-routes []

  [""
   ["/:pool_id"
    {:swagger {:conflicting true
               :tags ["Categories / Model-Groups"]}}


    ["/entitlement-groups"
     ["" {:get {:conflicting true
                :summary "OK | a.k.a 'Anspruchsgruppen' [fe]"
                :description (create-description "https://staging.leihs.zhdk.ch/manage/8bd16d45-056d-5590-bc7f-12849f034351/groups")
                :accept "application/json"
                :coercion reitit.coercion.schema/coercion
                :middleware [accept-json-middleware]
                :swagger {:produces ["application/json"]}
                :parameters {:path {:pool_id s/Uuid}}
                :handler get-entitlement-groups-of-pool-handler
                :responses {200 {:description "OK"
                                 :body [{:id s/Uuid
                                         :name s/Str
                                         :inventory_pool_id s/Uuid
                                         :is_verification_required s/Bool
                                         :created_at s/Any
                                         :updated_at s/Any}]}
                            404 {:description "Not Found"}
                            500 {:description "Internal Server Error"}}}}]

     ;["/:entitlement_group_id"
     ; {:get {:conflicting true
     ;        :summary "OK | a.k.a 'Anspruchsgruppen'"
     ;        :accept "application/json"
     ;        :coercion reitit.coercion.schema/coercion
     ;        :middleware [accept-json-middleware]
     ;        :swagger {:produces ["application/json"]}
     ;        :parameters {:path {:pool_id s/Uuid :entitlement_group_id s/Uuid}}
     ;        :handler get-entitlement-groups-of-pool-handler
     ;        :responses {200 {:description "OK"
     ;                         :body [{:id s/Uuid
     ;                                 :name s/Str
     ;                                 :inventory_pool_id s/Uuid
     ;                                 :is_verification_required s/Bool
     ;                                 :created_at s/Any
     ;                                 :updated_at s/Any}]}
     ;                    404 {:description "Not Found"}
     ;                    500 {:description "Internal Server Error"}}}}]


     ]]])
