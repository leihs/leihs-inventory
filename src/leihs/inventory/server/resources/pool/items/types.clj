(ns leihs.inventory.server.resources.pool.items.types
  (:require
   [schema.core :as s]))

(s/defschema query-params {(s/optional-key :fields) s/Str

                           (s/optional-key :search_term) s/Str
                           (s/optional-key :model_id) s/Uuid
                           (s/optional-key :parent_id) s/Uuid

                           ;; item filters
                           (s/optional-key :borrowable) s/Bool
                           (s/optional-key :broken) s/Bool
                           (s/optional-key :in_stock) s/Bool
                           (s/optional-key :incomplete) s/Bool
                           (s/optional-key :retired) s/Bool
                           (s/optional-key :owned) s/Bool

                           (s/optional-key :page) s/Int
                           (s/optional-key :size) s/Int})
