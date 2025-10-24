(ns leihs.inventory.server.resources.pool.options.routes
  (:require
   [leihs.inventory.server.resources.pool.options.main :as options]
   [leihs.inventory.server.resources.pool.options.types :as types :refer [response-option-get
                                                                          response-option-post]]
   [reitit.coercion.schema]
   [reitit.coercion.spec :as spec]
   [ring.middleware.accept]))

(defn routes []
  ["/options/"
   {:post {:accept "application/json"
           :coercion spec/coercion
           :parameters {:path {:pool_id uuid?}
                        :body :option/body}
           :produces ["application/json"]
           :handler options/post-resource
           :responses {200 {:description "OK"
                            :body response-option-post}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}

    :get {:accept "application/json"
          :coercion spec/coercion
          :parameters {:path {:pool_id uuid?}
                       :query ::types/options-query}
          :produces ["application/json"]
          :handler options/index-resources
          :responses {200 {:description "OK"
                           :body response-option-get}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}}])
