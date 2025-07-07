(ns leihs.inventory.server.resources.pool.fields.types
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.pool.fields.main :refer [get-form-fields-auto-pagination-handler]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(s/defschema query-params {(s/optional-key :page) s/Int
                           (s/optional-key :size) s/Int
                           (s/optional-key :role) (s/enum "inventory_manager" "lending_manager" "group_manager" "customer")
                           (s/optional-key :owner) s/Bool
                           (s/optional-key :type) (s/enum "license")})
