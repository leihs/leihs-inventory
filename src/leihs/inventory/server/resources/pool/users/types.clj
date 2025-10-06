(ns leihs.inventory.server.resources.pool.users.types
  (:require
   [clojure.set]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(def user-response-body [{:id s/Uuid
                          :firstname s/Str
                          :lastname s/Str
                          :login s/Str
                          :email s/Str
                          :searchable s/Str}])
