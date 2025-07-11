(ns leihs.inventory.server.resources.pool.models.model.types
  (:require
   [clojure.spec.alpha :as sa]
   ;[clojure.string :as str]
   [reitit.coercion.schema]
   [reitit.coercion.spec :as spec]
   ;[ring.middleware.accept]
   [schema.core :as s]))

(s/defschema patch-response {:id s/Uuid
                              :cover_image_id s/Uuid})

(def put-response :model-optional-response/inventory-model)

;(s/def ::deleted_model
;  (s/keys :req-un [::id ::product]
;    :opt-un [::manufacturer]))
;
;(s/def ::deleted_models (s/coll-of ::deleted_model))
;
;(s/defschema delete-response
;  {:deleted_attachments [{:id uuid?
;                          :model_id uuid?
;                          :filename string?
;                          :size number?}]
;   :deleted_images [any?]
;   ;:deleted_model
;   ::deleted_model}
;  ;::deleted_model
;  )


(sa/def ::id uuid?)
(sa/def ::model_id uuid?)
(sa/def ::product string?)
(sa/def ::filename  string?)
(sa/def ::manufacturer  string?)
(sa/def ::size  int?)

(def delete-response

;(sa/def ::delete-response
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