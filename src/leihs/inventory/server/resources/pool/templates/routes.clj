(ns leihs.inventory.server.resources.pool.templates.routes
  (:require
   [leihs.inventory.server.resources.pool.templates.main :as templates]
   [leihs.inventory.server.resources.pool.templates.types :as types]
   [reitit.coercion.spec :as spec]
   [ring.middleware.accept]))

(defn routes []
  ["/templates/"
   {:get {:accept "application/json"
          :coercion spec/coercion
          :description "- template_id == group_id"
          :parameters {:path {:pool_id uuid?}
                       :query ::types/get-query}
          :produces ["application/json"]
          :handler templates/index-resources
          :responses {200 {:description "OK"
                           :body ::types/get-response}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}

    :post {:accept "application/json"
           :coercion spec/coercion
           :description "- Duplicate entries allowed
\n- template_id == group_id
\n- Models can be determined by: /inventory/pool-id/list/?borrowable=true&type=model&page=1&retired=false"
           :parameters {:path {:pool_id uuid?}
                        :body ::types/post-query}
           :produces ["application/json"]
           :handler templates/post-resource
           :responses {200 {:description "OK"
                            :body ::types/post-response}
                       404 {:description "Not Found"}
                       409 {:description "Conflict"}
                       500 {:description "Internal Server Error"}}}}])
