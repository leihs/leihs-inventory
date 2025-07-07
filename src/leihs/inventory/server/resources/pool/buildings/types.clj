(ns leihs.inventory.server.resources.pool.buildings.types
  (:require
   ;[clojure.set]
   ;[leihs.inventory.server.constants :refer [fe]]
   ;[leihs.inventory.server.resources.pool.buildings.main :refer [get-buildings-handler ]]
   ;[leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   ;[leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   ;[ring.middleware.accept]
   [schema.core :as s]))

(s/defschema response-body {:id s/Uuid
                            :name s/Str
                            :code (s/maybe s/Str)})
