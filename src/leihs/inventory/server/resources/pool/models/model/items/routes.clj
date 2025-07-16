(ns leihs.inventory.server.resources.pool.models.model.items.routes
  (:require
   [leihs.inventory.server.resources.pool.models.model.items.main :as items]
   [leihs.inventory.server.resources.pool.models.model.items.types :refer [get-items-response]]
   [leihs.inventory.server.utils.middleware :refer [accept-json-middleware]]
   [reitit.coercion.schema]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  [""

   {:swagger {:tags [""]}}

   ["/models/:model_id"

    ["/items/"
     ["" {:get {:accept "application/json"
                :coercion reitit.coercion.schema/coercion
                :middleware [accept-json-middleware]
                :swagger {:produces ["application/json"]}
                :parameters {:path {:pool_id s/Uuid
                                    :model_id s/Uuid
                                    (s/optional-key :page) s/Int
                                    (s/optional-key :size) s/Int}}
                :handler items/index-resources
                :responses {200 {:description "OK"
                                 :body get-items-response}
                            404 {:description "Not Found"}
                            500 {:description "Internal Server Error"}}}}]]]])
