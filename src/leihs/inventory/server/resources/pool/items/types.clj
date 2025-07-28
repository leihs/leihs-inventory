(ns leihs.inventory.server.resources.pool.items.types
  (:require
   [schema.core :as s]))

(s/defschema query-params {(s/optional-key :page) s/Int
                           (s/optional-key :size) s/Int
                           (s/optional-key :search_term) s/Str
                           (s/optional-key :not_packaged) s/Bool
                           (s/optional-key :packages) s/Bool
                           (s/optional-key :retired) s/Bool
                           :result_type (s/enum "Min" "Normal" "Distinct")})
