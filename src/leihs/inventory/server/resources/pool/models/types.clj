(ns leihs.inventory.server.resources.pool.models.types
  (:require
   [leihs.inventory.server.resources.pool.models.coercion :as mc]
   [schema.core :as s]))

(def get-response mc/models-response-payload)
(def post-response :model-optional-response/inventory-model)
