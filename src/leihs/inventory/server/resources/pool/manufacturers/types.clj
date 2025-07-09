(ns leihs.inventory.server.resources.pool.manufacturers.types
  (:require [schema.core :as s]))

(s/defschema response-schema (s/conditional
                              map? {:id s/Uuid
                                    :manufacturer s/Str
                                    :product s/Str
                                    :version (s/maybe s/Str)
                                    :model_id s/Uuid}
                              string? s/Str))



