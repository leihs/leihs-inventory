(ns leihs.inventory.server.resources.pool.responsible-inventory-pools.types
  (:require
   [schema.core :as s]))

(def get-response
   [{:id s/Uuid
    :name s/Str
    :shortname s/Str}])
