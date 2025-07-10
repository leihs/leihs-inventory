(ns leihs.inventory.server.resources.pool.models.types
  (:require
   [leihs.inventory.server.resources.pool.models.coercion :as mc]
   [schema.core :as s]))

;(def get-response (s/->Either [{:data  [mc/models-response-payload] :pagination s/Any}  [mc/models-response-payload]]))
(def get-response {:data  [mc/models-response-payload] :pagination s/Any}  )
(def post-response :model-optional-response/inventory-model)
