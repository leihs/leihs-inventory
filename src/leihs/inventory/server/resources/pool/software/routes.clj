(ns leihs.inventory.server.resources.pool.software.routes
  (:require
   [clojure.spec.alpha :as sa]
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.software.main :as software]


   [leihs.inventory.server.resources.pool.models.basic_coercion :as sp]

   [leihs.inventory.server.resources.pool.software.types :as ty :refer [response-option-get
                                                                response-option-post]]
   [leihs.inventory.server.utils.auth.role-auth :refer [permission-by-role-and-pool]]
   [leihs.inventory.server.utils.auth.roles :as roles]
   [leihs.inventory.server.utils.coercion.core :refer [Date]]
   [leihs.inventory.server.utils.middleware :refer [accept-json-middleware]]
   [reitit.coercion.schema]
   [reitit.coercion.spec :as spec]
   [ring.middleware.accept]
   [schema.core :as s]))


;(sa/def :software/properties (sa/or
;                               :single (sa/or :coll (sa/coll-of ::sp/property)
;                                         :str string?)
;                               :none nil?))

(sa/def ::sp/image_attribute (sa/keys :req-opt [:image/filename
                                             :image/content_type
                                             :image/url
                                             :image/to_delete
                                             :image/thumbnail_url] :req-un [:image/id :image/is_cover]))

(sa/def :model/image_attributes (sa/or
                                  :single (sa/or :coll (sa/coll-of ::sp/image_attribute)
                                            :str string?)
                                  :none nil?))

(sa/def :software/multipart (sa/keys :req-un [::sp/product]
                              :opt-un [::sp/version
                                       ::sp/manufacturer
                                       ::sp/is_package
                                       ::sp/description
                                       ::sp/technical_detail
                                       ::sp/internal_description
                                       ::sp/hand_over_note
                                       ::sp/categories
                                       ::sp/attachments_to_delete
                                       ::sp/images_to_delete
                                       :model/image_attributes
                                       ::sp/owner
                                       ::sp/compatibles
                                       ::sp/images
                                       ::sp/attachments
                                       ::sp/entitlements
                                       :software/properties
                                       ::sp/accessories]))

(sa/def :software-post/multipart (sa/keys :req-un [::sp/product]
                              :opt-un [::sp/version
                                       ::sp/manufacturer
                                       ;::sp/is_package
                                       ::sp/description
                                       ;::sp/technical_detail
                                       ;::sp/internal_description
                                       ;::sp/hand_over_note
                                       ;::sp/categories
                                       ;::sp/attachments_to_delete
                                       ;::sp/images_to_delete
                                       ;:model/image_attributes
                                       ;::sp/owner
                                       ;::sp/compatibles
                                       ;::sp/images
                                       ;::sp/attachments
                                       ;::sp/entitlements
                                       ;:software/properties
                                       ;::sp/accessories
                                       ]))


(defn routes []
  ["/software/"
   {:post {:accept "application/json"
           :summary "(DEV) | Form-Handler: Fetch form data [v0]"
           :swagger {:consumes ["multipart/form-data"]
                     :produces "application/json"}
           :coercion spec/coercion
           :parameters {:path {:pool_id uuid?}
                        :multipart :software-post/multipart
                        }
           :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
           :handler software/index-resources
           :responses {200 {:description "OK"
                            ;:body any?
                            ;:body ::software-post/response
                            :body ::ty/post-response

                            }
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}])
