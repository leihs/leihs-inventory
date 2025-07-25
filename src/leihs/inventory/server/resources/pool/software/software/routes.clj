(ns leihs.inventory.server.resources.pool.software.software.routes
  (:require
   [clojure.spec.alpha :as sa]
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.software.software.main :as software]


   [leihs.inventory.server.resources.pool.models.basic_coercion :as sp]


   ;[leihs.inventory.server.resources.pool.software.software.types :refer [response-option-get
   ;                                                             response-option-post]]
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

(sa/def ::image_attribute (sa/keys :req-opt [:image/filename
                                             :image/content_type
                                             :image/url
                                             :image/to_delete
                                             :image/thumbnail_url] :req-un [:image/id :image/is_cover]))

(sa/def :model/image_attributes (sa/or
                                  :single (sa/or :coll (sa/coll-of ::image_attribute)
                                  ;:single (sa/or :coll (sa/coll-of any?)
                                            :str string?)
                                  :none nil?))


(sa/def :software/response
  (sa/keys :req-un [:nil/description
                    ::sp/is_package
                    ::sp/type
                    :nil/hand_over_note
                    :nil/internal_description
                    ::sp/product
                    ::sp/id
                    ::sp/manufacturer
                    :nil/version
                    :nil/technical_detail]

    :opt-un [::sp/attachments
             ::sp/maintenance_period
             :nil/rental_price
             :nil/cover_image_id
             ::sp/updated_at
             :nil/info_url
             ::sp/created_at]))


(def delete-response {:deleted_attachments [{:id uuid?
                                                      :model_id uuid?
                                                      :filename string?
                                                      :size number?}]
                               :deleted_model [{:id uuid?
                                                :product string?
                                                :manufacturer any?}]})

;(def delete-response
;  (sa/keys :req-un
;    [::deleted_attachments ::deleted_images ::deleted_model ::deleted_model_compatibles]))


(defn routes []
  ["/software/software/"
   {:get {:accept "application/json"
          :summary "(DEV) | Form-Handler: Fetch form data  [v0]"
          :coercion spec/coercion
          :parameters {:path {:pool_id uuid?
                              :model_id uuid?}}
          :handler software/create-software-handler-by-pool-form-fetch
          :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
          :responses {200 {:description "OK"
                           :body [:software/response]}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}

    :put {:accept "application/json"
          :swagger {:consumes ["multipart/form-data"]
                    :produces "application/json"}
          :summary "(DEV) | Form-Handler: Fetch form data [v0]"
          :coercion spec/coercion
          :parameters {:path {:pool_id uuid?
                              :model_id uuid?}
                       :multipart :software/multipart}
          :handler software/update-software-handler-by-pool-form
          :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
          :responses {200 {:description "OK"
                           :body :software/response
                           ;:body [:software/response]
                           }
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}

    :delete {:accept "application/json"
             :swagger {:consumes ["multipart/form-data"]
                       :produces "application/json"}
             :summary "(DEV) | Form-Handler: Delete form data [v0]"
             :coercion spec/coercion
             :parameters {:path {:pool_id uuid?
                                 :model_id uuid?}}
             :handler software/delete-software-handler-by-pool-form
             :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
             :responses {200 {:description "OK"
                              :body delete-response
                              ;:body {:deleted_attachments [{:id uuid?
                              ;                              :model_id uuid?
                              ;                              :filename string?
                              ;                              :size number?}]
                              ;       :deleted_model [{:id uuid?
                              ;                        :product string?
                              ;                        :manufacturer any?}]}
                              }
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}}])
