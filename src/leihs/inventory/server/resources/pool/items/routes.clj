(ns leihs.inventory.server.resources.pool.items.routes
  (:require
   [leihs.inventory.server.resources.pool.items.main :as items]
   [leihs.inventory.server.resources.pool.items.types :refer [query-params]]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  ["/items/"
   {:get {:accept "application/json"
          :coercion reitit.coercion.schema/coercion
          :swagger {:produces ["application/json"]}
          :parameters {:path {:pool_id s/Uuid}
                       :query query-params}
          :handler items/index-resources
          :produces ["application/json"]
          :responses {200 {:description "OK"
                           :body s/Any}
                        ;:body get-items-response} ;; FIXME broken
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}

    :post {:description "Create a new item. Fields starting with 'properties_' are stored in the properties JSONB column, others in their respective item columns."
           :accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :swagger {:produces ["application/json"]}
           :parameters {:path {:pool_id s/Uuid}
                        :body {(s/optional-key :inventory_code) s/Str
                               (s/optional-key :model_id) s/Uuid
                               (s/optional-key :room_id) s/Uuid
                               (s/optional-key :inventory_pool_id) s/Uuid
                               (s/optional-key :owner_id) s/Uuid
                               s/Keyword s/Any}}
           :handler items/post-resource
           :responses {200 {:description "OK"
                            :body s/Any}
                       400 {:description "Bad Request"}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}

    :put {:description "Update a single item. Fields starting with 'properties_' are stored in the properties JSONB column, others in their respective item columns."
          :accept "application/json"
          :coercion reitit.coercion.schema/coercion
          :swagger {:produces ["application/json"]}
          :parameters {:path {:pool_id s/Uuid}
                       :body {:id s/Uuid
                              s/Keyword s/Any}}
          :handler items/put-resource
          :responses {200 {:description "OK"
                           :body s/Any}
                      400 {:description "Bad Request"}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}}])
