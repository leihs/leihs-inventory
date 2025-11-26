(ns leihs.inventory.server.resources.pool.users.types
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.types :refer [pagination]]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(def user {:id s/Uuid
           :firstname s/Str
           :lastname s/Str
           :login (s/maybe s/Str)
           :email (s/maybe s/Str)
           :searchable s/Str})

(def user-response-body
  (s/->Either [[user] {:data [user] :pagination pagination}]))
