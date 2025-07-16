(ns leihs.inventory.server.resources.types
  (:require
   [schema.core :as s]))

(def pagination
  {
   :total_rows s/Int
   :total_pages s/Int
   :page s/Int
   :size s/Int
   })

