(ns leihs.inventory.server.resources.pool.fields.types
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.pool.fields.main :refer [index-resources]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(s/defschema query-params {:id s/Uuid
                           :name s/Str
                           :inventory_pool_id s/Uuid
                           :is_verification_required s/Bool
                           :created_at s/Any
                           :updated_at s/Any})

(def Permissions
  {(s/optional-key :role) s/Str
   (s/optional-key :owner) s/Bool})

(def Data
  {(s/optional-key :type) s/Str
   (s/optional-key :group) (s/maybe s/Str)
   (s/optional-key :label) s/Str
   (s/optional-key :required) s/Bool
   (s/optional-key :attribute) (s/cond-pre s/Str [s/Str])
   (s/optional-key :forPackage) s/Bool
   (s/optional-key :permissions) Permissions
   ;s/Keyword s/Any
}) ;; allow extra keys

(def ResponseItem
  {(s/optional-key :id) s/Str
   ;(s/optional-key :data) Data ;; FIXME
   (s/optional-key :data) s/Any
   (s/optional-key :role) (s/maybe s/Str)
   (s/optional-key :owner) (s/maybe s/Bool)})

(def get-response
  (s/->Either [{:data [ResponseItem] :pagination s/Any} [ResponseItem]]))
