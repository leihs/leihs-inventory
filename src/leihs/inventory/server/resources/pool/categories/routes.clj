(ns leihs.inventory.server.resources.pool.categories.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.pool.categories.main :refer [get-model-groups-of-pool-handler
                                                                  ]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn create-description [url]
  (str "- GET " url " Accept: application/json "))

(defn get-categories-routes []

  ;[""

  ["/:pool_id"
   {:swagger {:conflicting true
              :tags []}}

   ["/model-groups/"
    ["" {:get {:conflicting true
               :summary "a.k.a 'Categories'"
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
                                :body [{:id s/Uuid
                                        :type s/Str
                                        :name s/Str
                                        :created_at s/Any
                                        :updated_at s/Any}]}
                           404 {:description "Not Found"}
                           500 {:description "Internal Server Error"}}}}]

    ]

   ])
