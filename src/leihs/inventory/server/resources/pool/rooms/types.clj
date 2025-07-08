(ns leihs.inventory.server.resources.pool.rooms.types
  (:require
   [schema.core :as s]))

(s/defschema get-response {:building_id s/Uuid
              :description (s/maybe s/Str)
              :id s/Uuid
              :name s/Str
              :general s/Bool})
