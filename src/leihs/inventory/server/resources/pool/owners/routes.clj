(ns leihs.inventory.server.resources.pool.owners.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.owners.main :as owners]
   [leihs.inventory.server.resources.pool.owners.types :refer [response-body]]
   [leihs.inventory.server.utils.middleware :refer [accept-json-middleware]]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(def resp-owners [{:id s/Uuid
                   :name s/Str}])

(def pagination {:size s/Int
                 :page s/Int
                 :total_rows s/Int
                 :total_pages s/Int})

(defn routes []
  ["/owners/"
   {:get {:accept "application/json"
          :coercion reitit.coercion.schema/coercion
          :parameters {:path {:pool_id s/Uuid}
                       :query {(s/optional-key :page) s/Int
                               (s/optional-key :size) s/Int}}
          :handler owners/index-resources
          :responses {200 {:description "OK"
                           :body (s/->Either [resp-owners {:data resp-owners
                                                           :pagination pagination}])}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}}])
