(ns leihs.inventory.server.resources.pool.models.model.types
  (:require
   [clojure.spec.alpha :as sa]
   [clojure.string :as str]
   ;[leihs.inventory.server.constants :refer [fe]]
   ;[leihs.inventory.server.resources.pool.models.coercion :as mc]
   ;[leihs.inventory.server.resources.pool.models.model.common-model-form :refer [patch-model-handler]]
   ;[leihs.inventory.server.resources.pool.models.model.create-model-form :refer [create-model-handler-by-pool-model-json]]
   ;[leihs.inventory.server.resources.pool.models.model.delete-model-form :refer [delete-model-handler-by-pool-json]]
   ;[leihs.inventory.server.resources.pool.models.model.fetch-model-form :refer [create-model-handler-by-pool-form-fetch]]
   ;[leihs.inventory.server.resources.pool.models.model.main :refer [update-model-handler
   ;                                                                 delete-model-handler
   ;                                                                 get-models-handler]]
   ;[leihs.inventory.server.resources.pool.models.model.update-model-form :refer [update-model-handler-by-pool-model-json]]
   ;[leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   ;[leihs.inventory.server.utils.auth.role-auth :refer [permission-by-role-and-pool]]
   ;[leihs.inventory.server.utils.auth.roles :as roles]
   ;[leihs.inventory.server.utils.coercion.core :refer [Date]]
   ;[leihs.inventory.server.utils.constants :refer [config-get]]
   [reitit.coercion.schema]
   [reitit.coercion.spec :as spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(s/defschema patch-response [{:id s/Uuid
                              :cover_image_id s/Uuid}])

(s/defschema put-response {:data :model-optional-response/inventory-model
                           :validation any?})

(s/defschema delete-response {:deleted_attachments [{:id uuid?
                                                     :model_id uuid?
                                                     :filename string?
                                                     :size number?}]
                              :deleted_images [any?]
                              :deleted_model [{:id uuid?
                                               :product string?
                                               :manufacturer any?}]})