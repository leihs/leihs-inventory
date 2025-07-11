(ns leihs.inventory.server.resources.pool.models.model.types
  (:require
   [clojure.spec.alpha :as sa]
   [reitit.coercion.schema]
   [reitit.coercion.spec :as spec]
   [schema.core :as s]))

(s/defschema patch-response {:id s/Uuid
                             :cover_image_id s/Uuid})

(def put-response :model-optional-response/inventory-model)

(sa/def ::id uuid?)
(sa/def ::model_id uuid?)
(sa/def ::product string?)
(sa/def ::filename string?)
(sa/def ::manufacturer string?)
(sa/def ::size int?)

(def delete-response

  (sa/keys :req-un
           [::deleted_attachments ::deleted_images ::deleted_model]))

(sa/def ::deleted_attachments
  (sa/coll-of
   (sa/keys :req-un [::id ::model_id ::filename ::size])))

(sa/def ::deleted_images (sa/coll-of any?))

(sa/def ::deleted_model
  (sa/coll-of
   (sa/keys :req-un [::id ::product]
            :opt-un [::manufacturer])))