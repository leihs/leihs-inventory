(ns leihs.inventory.server.resources.pool.fields.types
  (:require
   [schema.core :as s]))

(def Field
  {:id s/Str
   :position s/Int
   :label s/Str
   :type s/Str
   :group (s/maybe s/Str)})
