(ns leihs.inventory.server.resources.pool.fields.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.pool.fields.main :as fields]
   [leihs.inventory.server.resources.pool.fields.types :as types]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  ["/fields/"
   {:get {:accept "application/json"
          :coercion reitit.coercion.schema/coercion
          :swagger {:produces ["application/json"]}
          :parameters {:path {:pool_id s/Uuid}
                       :query {:target_type (s/enum "item" "license" "package")}}
          :handler fields/index-resources
          :responses {200 {:description "OK"
                           :body {:fields [types/Field]}}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}}])
