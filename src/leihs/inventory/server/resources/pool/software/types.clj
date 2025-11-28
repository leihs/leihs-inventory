(ns leihs.inventory.server.resources.pool.software.types
  (:require
   [clojure.spec.alpha :as sa]
   [leihs.inventory.server.resources.pool.models.basic-coercion :as sp]
   [leihs.inventory.server.resources.types :refer [pagination]]
   [reitit.coercion.schema]
   [schema.core :as s]))

(sa/def :software-post/multipart (sa/keys :req-un [::sp/product]
                                          :opt-un [:nil/version
                                                   :nil/manufacturer
                                                   ::sp/technical_detail]))

(sa/def ::post-response
  (sa/keys :req-un [:models/type
                    ::sp/product
                    ::sp/id
                    :nil/manufacturer
                    :nil/version]
           :opt-un [:nil/technical_detail
                    ::sp/attachments]))

(def index-resource
  {:id s/Uuid
   :product (s/maybe s/Str)
   :version (s/maybe s/Str)
   :name (s/maybe s/Str)
   :cover_image_id (s/maybe s/Uuid)
   (s/optional-key :available) (s/maybe s/Int)

   (s/optional-key :url) (s/maybe s/Str)
   (s/optional-key :content_type) (s/maybe s/Str)
   (s/optional-key :image_id) (s/maybe s/Uuid)})

(def index-resources
  (s/->Either [[index-resource] {:data [index-resource] :pagination pagination}]))
