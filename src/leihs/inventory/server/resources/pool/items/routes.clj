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
   {:get {:description "https://staging.leihs.zhdk.ch/manage/8bd16d45-056d-5590-bc7f-12849f034351/items"

          :accept "application/json"
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
                      500 {:description "Internal Server Error"}}}}])
