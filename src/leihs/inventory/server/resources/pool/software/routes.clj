(ns leihs.inventory.server.resources.pool.software.routes
  (:require
   [leihs.inventory.server.resources.pool.software.main :as software]
   [leihs.inventory.server.resources.pool.software.types :as types]
   [reitit.coercion.spec :as spec]
   [ring.middleware.accept]))

(defn routes []
  ["/software/"
   {:post {:accept "application/json"
           :coercion spec/coercion
           :parameters {:path {:pool_id uuid?}
                        :body :software-post/multipart}
           :produces ["application/json"]
           :handler software/post-resource
           :responses {200 {:description "OK"
                            :body ::types/post-response}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}])
