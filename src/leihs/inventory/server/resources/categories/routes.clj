(ns leihs.inventory.server.resources.categories.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.categories.main :refer [get-model-groups-of-pool-handler
                                                             get-groups-of-pool-handler
                                                             get-entitlement-groups-of-pool-handler
                                                             get-model-group-links-of-pool-handler]]
   [leihs.inventory.server.resources.models.main :refer [get-models-handler
                                                         create-model-handler
                                                         update-model-handler
                                                         delete-model-handler]]
   [leihs.inventory.server.resources.models.models-by-pool :refer [get-models-of-pool-handler
                                                                   create-model-handler-by-pool
                                                                   get-models-of-pool-handler
                                                                   update-model-handler-by-pool
                                                                   delete-model-handler-by-pool]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn create-description [url]
  (str "- GET " url " Accept: application/json "))

(defn get-categories-routes []

  [""

   ["/category_links"
    {:swagger {:conflicting true
               :tags ["Category-Links"] :security []}}
    ["" {:get {:conflicting true
               :summary "OK | Kategorie-Links anzeigen / category_links == model_group_links"
               :description (create-description "https://staging.leihs.zhdk.ch/category_links")
               :accept "application/json"
               :coercion reitit.coercion.schema/coercion
               :middleware [accept-json-middleware]
               :swagger {:produces ["application/json"]}
              ;:parameters {:path {:pool_id s/Uuid}}
               :handler get-model-group-links-of-pool-handler
               :responses {200 {:description "OK"
                                :body s/Any}
                           404 {:description "Not Found"}
                           500 {:description "Internal Server Error"}}}}]

    ["/:category_link_id"
     {:get {:conflicting true
            :summary "Kategorie-Links anzeigen / category_links"
            :accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :middleware [accept-json-middleware]
            :swagger {:produces ["application/json"]}
            :parameters {:path {;:pool_id s/Uuid
                                :category_link_id s/Uuid}}
            :handler get-model-group-links-of-pool-handler
            :responses {200 {:description "OK"
                             :body s/Any}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]]

   ["/:pool_id"
    {:swagger {:conflicting true
               :tags ["Categories / Model-Groups"] :security []}}

    ["/model-groups"
     ["" {:get {:conflicting true
                :summary "OK | a.k.a 'Categories'"
                :description (str (create-description "https://staging.leihs.zhdk.ch/manage/8bd16d45-056d-5590-bc7f-12849f034351/categories?search_term=")
                                  " - FYI: pool_id is not used by query")
                :accept "application/json"
               ;; TODO: add name-filter and pagination, used?-attribute
                :coercion reitit.coercion.schema/coercion
                :middleware [accept-json-middleware]
                :swagger {:produces ["application/json"]}
                :parameters {:path {:pool_id s/Uuid}}
                :handler get-model-groups-of-pool-handler
                :responses {200 {:description "OK"
                                 :body s/Any}
                            404 {:description "Not Found"}
                            500 {:description "Internal Server Error"}}}}]

     ["/:model_group_id"
      {:get {:conflicting true
             :summary "OK | a.k.a 'Categories'"
             :accept "application/json"
             :coercion reitit.coercion.schema/coercion
             :middleware [accept-json-middleware]
             :swagger {:produces ["application/json"]}
             :parameters {:path {:pool_id s/Uuid :model_group_id s/Uuid}}
             :handler get-model-groups-of-pool-handler
             :responses {200 {:description "OK"
                              :body s/Any}
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}}]]

    ["/groups"
     ["" {:get {:conflicting true
                :accept "application/json"
                :coercion reitit.coercion.schema/coercion
                :middleware [accept-json-middleware]
                :swagger {:produces ["application/json"]}
                :parameters {:path {:pool_id s/Uuid}}
                :handler get-groups-of-pool-handler
                :responses {200 {:description "OK"
                                 :body s/Any}
                            404 {:description "Not Found"}
                            500 {:description "Internal Server Error"}}}}]

     ["/:group_id"
      {:get {:conflicting true
             :accept "application/json"
             :coercion reitit.coercion.schema/coercion
             :middleware [accept-json-middleware]
             :swagger {:produces ["application/json"]}
             :parameters {:path {:pool_id s/Uuid :group_id s/Uuid}}
             :handler get-groups-of-pool-handler
             :responses {200 {:description "OK"
                              :body s/Any}
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}}]]

    ["/entitlement-groups"
     ["" {:get {:conflicting true
                :summary "OK | a.k.a 'Anspruchsgruppen'"
                :description (create-description "https://staging.leihs.zhdk.ch/manage/8bd16d45-056d-5590-bc7f-12849f034351/groups")
                :accept "application/json"
                :coercion reitit.coercion.schema/coercion
                :middleware [accept-json-middleware]
                :swagger {:produces ["application/json"]}
                :parameters {:path {:pool_id s/Uuid}}
                :handler get-entitlement-groups-of-pool-handler
                :responses {200 {:description "OK"
                                 :body s/Any}
                            404 {:description "Not Found"}
                            500 {:description "Internal Server Error"}}}}]

     ["/:entitlement_group_id"
      {:get {:conflicting true
             :summary "OK | a.k.a 'Anspruchsgruppen'"
             :accept "application/json"
             :coercion reitit.coercion.schema/coercion
             :middleware [accept-json-middleware]
             :swagger {:produces ["application/json"]}
             :parameters {:path {:pool_id s/Uuid :entitlement_group_id s/Uuid}}
             :handler get-entitlement-groups-of-pool-handler
             :responses {200 {:description "OK"
                              :body s/Any}
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}}]]]])
