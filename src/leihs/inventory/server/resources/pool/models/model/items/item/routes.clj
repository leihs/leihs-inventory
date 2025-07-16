(ns leihs.inventory.server.resources.pool.models.model.items.item.routes
  (:require
   [leihs.inventory.server.resources.pool.models.model.items.item.main :as item]
   [leihs.inventory.server.resources.pool.models.model.items.item.types :refer [get-item-response]]
   [leihs.inventory.server.utils.middleware :refer [accept-json-middleware]]
   [reitit.coercion.schema]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  [""

   {:swagger {:tags [""]}}

   ["/models/:model_id"

    ["/items/"

     [":item_id"
      {:get {:accept "application/json"
             :coercion reitit.coercion.schema/coercion
             :middleware [accept-json-middleware]
             :swagger {:produces ["application/json"]}
             :parameters {:path {:pool_id s/Uuid
                                 :model_id s/Uuid
                                 :item_id s/Uuid}}
             :handler item/get-resource
             :responses {200 {:description "OK"
                              ;:body (s/->Either [s/Any schema])} ;;FIXME
                              :body get-item-response}
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}}]]]])
