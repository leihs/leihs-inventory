(ns leihs.inventory.server.resources.pool.models.model.items.routes
  (:require
   [leihs.inventory.server.resources.pool.models.model.items.main :as items]
   [leihs.inventory.server.resources.pool.models.model.items.types :refer [get-items-response]]
   [reitit.coercion.schema]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  ["/models/:model_id/items/"
   ["" {:get {:accept "application/json"
              :coercion reitit.coercion.schema/coercion
              :swagger {:produces ["application/json"]}
              :parameters {:path {:pool_id s/Uuid
                                  :model_id s/Uuid
                                  (s/optional-key :page) s/Int
                                  (s/optional-key :size) s/Int}}
              :produces ["application/json"]
              :handler items/index-resources
              :responses {200 {:description "OK"
                               :body get-items-response}
                          404 {:description "Not Found"}
                          500 {:description "Internal Server Error"}}}}]])
