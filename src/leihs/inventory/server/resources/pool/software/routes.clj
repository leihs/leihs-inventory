(ns leihs.inventory.server.resources.pool.software.routes
  (:require
   [leihs.inventory.server.resources.pool.software.main :as software]
   [leihs.inventory.server.resources.pool.software.types :as types]
   [reitit.coercion.schema]
   [reitit.coercion.spec :as spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  ["/software/"
   {:get {:conflicting true
          :accept "application/json"
          :coercion reitit.coercion.schema/coercion
          :swagger {:produces ["application/json"]}
          :parameters {:path {:pool_id s/Uuid}
                       :query {(s/optional-key :page) s/Int
                               (s/optional-key :size) s/Int
                               (s/optional-key :search) s/Str
                               (s/optional-key :search_term) s/Str}}
          :handler software/index-resources
          :responses {200 {:description "OK"
                           :body types/index-resources}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}

    :post {:accept "application/json"
           :coercion spec/coercion
           :parameters {:path {:pool_id uuid?}
                        :body :software-post/multipart}
           :produces ["application/json"]
           :handler software/post-resource
           :responses {200 {:description "OK"
                            :body ::types/post-response}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}])
