(ns leihs.inventory.server.resources.pool.models.model.types
  (:require
   ;[clojure.spec.alpha :as sa]
   ;[clojure.string :as str]
   [reitit.coercion.schema]
   [reitit.coercion.spec :as spec]
   ;[ring.middleware.accept]
   [schema.core :as s]))

(s/defschema patch-response {:id s/Uuid
                              :cover_image_id s/Uuid})

(def put-response :model-optional-response/inventory-model)

(s/defschema delete-response {:deleted_attachments [{:id uuid?
                                                     :model_id uuid?
                                                     :filename string?
                                                     :size number?}]
                              :deleted_images [any?]
                              :deleted_model [{:id uuid?
                                               :product string?
                                               :manufacturer any?}]})