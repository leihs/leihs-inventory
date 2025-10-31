(ns leihs.inventory.server.resources.pool.suppliers.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.pool.suppliers.main :as suppliers]
   [leihs.inventory.server.resources.pool.suppliers.types :refer [get-response]]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  ["/suppliers/"
   {:get {:description "- DEFAULT: no pagination"
          :accept "application/json"
          :coercion reitit.coercion.schema/coercion
          :swagger {:produces ["application/json"]}
          :parameters {:query {(s/optional-key :page) s/Int
                               (s/optional-key :size) s/Int
                               (s/optional-key :search) s/Str}}
          :produces ["application/json"]
          :handler suppliers/index-resources
          :responses {200 {:description "OK"
                           :body get-response}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}}])
