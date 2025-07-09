(ns leihs.inventory.server.resources.pool.rooms.types
  (:require
   ;[clojure.set]
   ;[leihs.inventory.server.constants :refer [fe]]
   ;[leihs.inventory.server.resources.pool.rooms.main :refer [index-resources]]
   ;[leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   ;[leihs.inventory.server.utils.response_helper :as rh]
   ;[reitit.coercion.schema]
   ;[reitit.coercion.spec]
   ;[ring.middleware.accept]
   [schema.core :as s]))


(s/defschema get-response {:building_id s/Uuid
              :description (s/maybe s/Str)
              :id s/Uuid
              :name s/Str
              :general s/Bool})


