(ns leihs.inventory.server.resources.pool.owners.types
  (:require
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [schema.core :as s]))

(s/defschema response-body {:id s/Uuid
                            :name s/Str
                            :code (s/maybe s/Str)})
