(ns leihs.inventory.server.resources.pool.fields.types
  (:require
   [schema.core :as s]))

(def Field
  {:id s/Str
   :position s/Int
   :label s/Str
   :type s/Str
   (s/optional-key :group) (s/maybe s/Str)
   :required s/Bool
   (s/optional-key :default) (s/maybe s/Any)
   (s/optional-key :values) (s/conditional vector? [s/Any]
                                           string? s/Str)
   (s/optional-key :visibility_dependency_value) s/Str
   (s/optional-key :visibility_dependency_field_id) s/Str
   (s/optional-key :form_name) s/Str
   (s/optional-key :search_attr) s/Str
   (s/optional-key :value_attr) s/Str
   (s/optional-key :search_path) s/Str})
