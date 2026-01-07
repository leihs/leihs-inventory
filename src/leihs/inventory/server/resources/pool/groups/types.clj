(ns leihs.inventory.server.resources.pool.groups.types
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.types :refer [pagination]]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(def group {:id s/Uuid
            :name s/Str
            :searchable s/Str
            :user_count s/Int})

(def group-response-body
  (s/->Either [[group] {:data [group] :pagination pagination}]))
