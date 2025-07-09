(ns leihs.inventory.server.resources.pool.manufacturers.types
  (:require
   ;[clojure.spec.alpha :as sa]
   ;[clojure.string :as str]
   ;[leihs.inventory.server.constants :refer [fe]]
   ;[leihs.inventory.server.resources.pool.manufacturers.main :refer [index-resources]]
   ;[leihs.inventory.server.resources.pool.models.coercion :as mc]
   ;[leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   ;[leihs.inventory.server.utils.auth.role-auth :refer [permission-by-role-and-pool]]
   ;[leihs.inventory.server.utils.auth.roles :as roles]
   ;[leihs.inventory.server.utils.coercion.core :refer [Date]]
   ;[leihs.inventory.server.utils.constants :refer [config-get]]
   ;[reitit.coercion.schema]
   ;[reitit.coercion.spec :as spec]
   ;[ring.middleware.accept]
   [schema.core :as s]))

(s/defschema response-schema (s/conditional
                                    map? {:id s/Uuid
                                          :manufacturer s/Str
                                          :product s/Str
                                          :version (s/maybe s/Str)
                                          :model_id s/Uuid}
                                    string? s/Str))



