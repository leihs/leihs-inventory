(ns leihs.inventory.server.resources.categories.category.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.categories.category.main :refer [get-model-groups-of-pool-handler
                                                             ;get-groups-of-pool-handler
                                                             ;get-entitlement-groups-of-pool-handler
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

(defn get-categories-category-route []

  ;[""




   ["/:pool_id"
    {:swagger {:conflicting true
               :tags ["Categories / Model-Groups"]}}

    ;["/model-groups"
    ; ["" {:get {:conflicting true
    ;            :summary "OK | a.k.a 'Categories'"
    ;            :description (str (create-description "https://staging.leihs.zhdk.ch/manage/8bd16d45-056d-5590-bc7f-12849f034351/categories?search_term=")
    ;                              " - FYI: pool_id is not used by query")
    ;            :accept "application/json"
    ;           ;; TODO: add name-filter and pagination, used?-attribute
    ;            :coercion reitit.coercion.schema/coercion
    ;            :middleware [accept-json-middleware]
    ;            :swagger {:produces ["application/json"]}
    ;            :parameters {:path {:pool_id s/Uuid}}
    ;            :handler get-model-groups-of-pool-handler
    ;            :responses {200 {:description "OK"
    ;                             :body [{:id s/Uuid
    ;                                     :type s/Str
    ;                                     :name s/Str
    ;                                     :created_at s/Any
    ;                                     :updated_at s/Any}]}
    ;                        404 {:description "Not Found"}
    ;                        500 {:description "Internal Server Error"}}}}]

     ["/model-groups/:model_group_id"
      {:get {:conflicting true
             :summary "OK | a.k.a 'Categories'"
             :accept "application/json"
             :coercion reitit.coercion.schema/coercion
             :middleware [accept-json-middleware]
             :swagger {:produces ["application/json"]}
             :parameters {:path {:pool_id s/Uuid :model_group_id s/Uuid}}
             :handler get-model-groups-of-pool-handler
             :responses {200 {:description "OK"
                              :body [{:id s/Uuid
                                      :type s/Str
                                      :name s/Str
                                      :created_at s/Any
                                      :updated_at s/Any}]}
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}}]]



   ;]



   ;]
)
