(ns leihs.inventory.server.resources.pool.users.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.pool.users.main :as users]
   [leihs.inventory.server.resources.pool.users.types :as types]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  ["/users/" {:get {:accept "application/json"
                    :coercion reitit.coercion.schema/coercion
                    :swagger {:produces ["application/json"]}
                    :produces ["application/json"]
                    :parameters {:path {:pool_id s/Uuid}
                                 :query {(s/optional-key :page) s/Int
                                         (s/optional-key :size) s/Int
                                         (s/optional-key :search) s/Str
                                         (s/optional-key :account_enabled) s/Bool}}
                    :handler users/index-resources
                    :responses {200 {:description "OK"
                                     :body types/user-response-body}
                                404 {:description "Not Found"}
                                500 {:description "Internal Server Error"}}}}])
