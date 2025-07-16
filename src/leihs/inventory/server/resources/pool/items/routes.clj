(ns leihs.inventory.server.resources.pool.items.routes
  (:require
   [cheshire.core :as json]
   [clojure.set :as set]
   [leihs.inventory.server.resources.pool.items.main :as items]
   [leihs.inventory.server.resources.pool.items.types :refer [query-params]]
   [leihs.inventory.server.resources.pool.models.model.items.types :refer [get-items-response]]
   [leihs.inventory.server.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [ring.util.response :as response]
   [schema.core :as s]))

(defn routes []
  [""

   [""
    {:swagger {:tags [""]}}

    ["/items/"

     {:get {:description "https://staging.leihs.zhdk.ch/manage/8bd16d45-056d-5590-bc7f-12849f034351/items"

            :accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :middleware [accept-json-middleware]
            :swagger {:produces ["application/json"]}
            :parameters {:path {:pool_id s/Uuid}
                         :query query-params}
            :handler items/index-resources
            :responses {200 {:description "OK"
                             :body s/Any}
                        ;:body get-items-response} ;; FIXME broken
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]]])
