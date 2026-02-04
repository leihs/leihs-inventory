(ns leihs.inventory.server.resources.pool.items.routes
  (:require
   [leihs.inventory.server.resources.pool.items.main :as items]
   [leihs.inventory.server.resources.pool.items.types :as types]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s])
  (:import [java.io InputStream]))

(defn routes []
  ["/items/"
   {:get {:accept "application/json"
          :coercion reitit.coercion.schema/coercion
          :swagger {:produces ["application/json"
                               "text/csv"
                               "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"]}
          :summary "Returns all items/packages of a pool filtered by query parameters"
          :parameters {:path types/path-params
                       :query types/query-params}
          :handler items/index-resources
          :produces ["application/json"
                     "text/csv"
                     "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"]
          :responses {200 {:description "OK"
                           :body (s/conditional
                                  string? s/Str
                                  #(instance? InputStream %) s/Any
                                  :else types/get-items-response)}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}

    :post {:description "Create one or more items. When 'count' is provided, creates multiple items with auto-generated sequential inventory codes. Otherwise, creates a single item with the provided inventory_code. Fields starting with 'properties_' are stored in the properties JSONB column, others in their respective item columns."
           :accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :swagger {:produces ["application/json"]}
           :parameters {:path {:pool_id s/Uuid}
                        :body types/post-request}
           :handler items/post-resource
           :responses {200 {:description "OK - Returns single item or array of items"
                            :body types/post-response}
                       400 {:description "Bad Request"}
                       404 {:description "Not Found"}
                       409 {:description "Conflict - Inventory code already exists"}
                       500 {:description "Internal Server Error"}}}}])
