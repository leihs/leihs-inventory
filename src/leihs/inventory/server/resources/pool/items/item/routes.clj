(ns leihs.inventory.server.resources.pool.items.item.routes
  (:require
   [leihs.inventory.server.resources.pool.items.item.main :as item]
   [leihs.inventory.server.resources.pool.items.item.types :as types]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  ["/items/:item_id"
   {:get {:description "Get a single item by its ID."
          :accept "application/json"
          :coercion reitit.coercion.schema/coercion
          :swagger {:produces ["application/json"]}
          :parameters {:path {:pool_id s/Uuid
                              :item_id s/Uuid}}
          :handler item/get-resource
          :responses {200 {:description "OK"
                           :body types/get-response}
                      400 {:description "Bad Request"}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}

    :patch {:description "Update a single item. Fields starting with 'properties_' are stored in the properties JSONB column, others in their respective item columns."
            :accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :swagger {:produces ["application/json"]}
            :parameters {:path {:pool_id s/Uuid
                                :item_id s/Uuid}
                         :body types/patch-request}
            :handler item/patch-resource
            :responses {200 {:description "OK"
                             :body types/patch-response}
                        400 {:description "Bad Request"}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}])
