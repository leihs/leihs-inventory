(ns leihs.inventory.server.resources.pool.models.model.items.item.routes
  (:require
   [leihs.inventory.server.resources.pool.models.model.items.item.main :as item]
   [leihs.inventory.server.resources.pool.models.model.items.item.types :refer [get-item-response]]
   [reitit.coercion.schema]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  ["/models/:model_id/items/:item_id"
   {:get {:accept "application/json"
          :coercion reitit.coercion.schema/coercion
          :swagger {:produces ["application/json"]}
          :parameters {:path {:pool_id s/Uuid
                              :model_id s/Uuid
                              :item_id s/Uuid}}
          :produces ["application/json"]
          :handler item/get-resource
          :responses {200 {:description "OK"
                              ;:body (s/->Either [s/Any schema])} ;;FIXME
                           :body get-item-response}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}}])
