(ns leihs.inventory.server.resources.pool.models.routes
  (:require
   [clojure.spec.alpha :as sa]
   [clojure.string :as str]
   [leihs.inventory.server.resources.pool.accessories.main :refer [get-accessories-of-pool-handler]]
   [leihs.inventory.server.resources.pool.attachments.main :refer [delete-attachments]]
   [leihs.inventory.server.resources.pool.models.coercion :as mc]
   [leihs.inventory.server.resources.pool.models.entitlements.main :refer [get-entitlements-with-pagination-handler]]
   [leihs.inventory.server.resources.pool.models.form.items.model-by-pool-form-create :refer [create-items-handler-by-pool-form]]
   [leihs.inventory.server.resources.pool.models.form.items.model-by-pool-form-fetch :refer [fetch-items-handler-by-pool-form]]
   [leihs.inventory.server.resources.pool.models.form.items.model-by-pool-form-update :refer [update-items-handler-by-pool-form]]
   [leihs.inventory.server.resources.pool.models.form.license.model-by-pool-form-create :refer [create-license-handler-by-pool-form]]
   [leihs.inventory.server.resources.pool.models.form.license.model-by-pool-form-fetch :refer [fetch-license-handler-by-pool-form-fetch]]
   [leihs.inventory.server.resources.pool.models.form.license.model-by-pool-form-update :refer [update-license-handler-by-pool-form]]
   [leihs.inventory.server.resources.pool.models.form.model.common :refer [delete-image
                                                                      patch-model-handler
                                                                      patch-models-handler
                                                                      upload-attachment
                                                                      upload-image]]
   [leihs.inventory.server.resources.pool.models.form.model.model-by-pool-json-create :refer [create-model-handler-by-pool-model-json]]
   [leihs.inventory.server.resources.pool.models.form.model.model-by-pool-json-delete :refer [delete-model-handler-by-pool-json]]
   [leihs.inventory.server.resources.pool.models.form.model.model-by-pool-json-fetch :refer [create-model-handler-by-pool-form-fetch]]
   [leihs.inventory.server.resources.pool.models.form.model.model-by-pool-json-update :refer [update-model-handler-by-pool-model-json]]
   [leihs.inventory.server.resources.pool.models.form.option.model-by-pool-form-create :refer [create-option-handler-by-pool-form]]
   [leihs.inventory.server.resources.pool.models.form.option.model-by-pool-form-fetch :refer [fetch-option-handler-by-pool-form]]
   [leihs.inventory.server.resources.pool.models.form.option.model-by-pool-form-update :refer [update-option-handler-by-pool-form]]
   [leihs.inventory.server.resources.pool.models.form.package.model-by-pool-form-create :refer [create-package-handler-by-pool-form]]
   [leihs.inventory.server.resources.pool.models.form.package.model-by-pool-form-fetch :refer [fetch-package-handler-by-pool-form]]
   [leihs.inventory.server.resources.pool.models.form.package.model-by-pool-form-update :refer [update-package-handler-by-pool-form]]
   [leihs.inventory.server.resources.pool.models.form.software.model-by-pool-form-create :refer [create-software-handler-by-pool-form]]
   [leihs.inventory.server.resources.pool.models.form.software.model-by-pool-form-fetch :refer [create-software-handler-by-pool-form-fetch]]
   [leihs.inventory.server.resources.pool.models.form.software.model-by-pool-form-update :refer [delete-software-handler-by-pool-form
                                                                                            update-software-handler-by-pool-form]]
   [leihs.inventory.server.resources.pool.models.inventory-list :refer [inventory-list-handler]]
   [leihs.inventory.server.resources.pool.models.items.main :refer [get-items-with-pagination-handler]]
   ;[leihs.inventory.server.resources.pool.models._main :refer [create-model-handler
   ;                                                      delete-model-handler
   ;                                                      get-manufacturer-handler
   ;                                                      get-models-compatible-handler
   ;                                                      update-model-handler]]
   [leihs.inventory.server.resources.pool.models.model-links.main :refer [get-model-links-with-pagination-handler]]
   [leihs.inventory.server.resources.pool.models.models-by-pool :refer [create-model-handler-by-pool
                                                                   delete-model-handler-by-pool
                                                                   get-models-handler
                                                                   get-models-of-pool-auto-pagination-handler
                                                                   get-models-of-pool-handler
                                                                   get-models-of-pool-with-pagination-handler
                                                                   update-model-handler-by-pool]]
   [leihs.inventory.server.resources.pool.models.properties.main :refer [get-properties-with-pagination-handler]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.auth.role-auth :refer [permission-by-role-and-pool]]
   [leihs.inventory.server.utils.auth.roles :as roles]
   [leihs.inventory.server.utils.coercion.core :refer [Date]]
   [leihs.inventory.server.utils.constants :refer [config-get]]
   [reitit.coercion.schema]
   [reitit.coercion.spec :as spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(def description-model-form "CAUTION:\n
- Model\n
   - Modifies all attributes except: Images/Attachments\n
   - Use PATCH /inventory/<pool-id>/model/<image-id> to set is_cover\n
   - GET: contains all data for fields (attachment, image included)\n
- Full sync will be processed for: accessories, compatibles, categories, entitlements, properties\n
- Image\n
   - Use POST /inventory/models/<model-id>/images to upload image\n
   - Use DELETE /inventory/models/<model-id>/images/<image-id> to delete image\n
- Attachment\n
   - Use POST /inventory/models/<model-id>/attachments to upload attachment\n
   - Use DELETE /inventory/models/<model-id>/attachments/<attachment-id> to delete attachment")

(def compatible-data {(s/optional-key :cover_image_id) s/Uuid
                      (s/optional-key :cover_image_url) s/Str
                      :model_id s/Any
                      :product s/Str
                      (s/optional-key :version) s/Str})

(def compatible-response
  (s/->Either [[compatible-data] {:data [compatible-data] :pagination s/Any}]))

(def FileUpload
  "Schema describing a typical Ring multipart file map."
  {:filename s/Str
   :size s/Int
   :content-type s/Str
   :tempfile s/Any})

(defn get-model-route []
  ["/"
   {:swagger {:conflicting true
              :tags ["Models"]}}

   ["manufacturers"
    {:get {:conflicting true
           :summary "Get manufacturers [fe]"
           :accept "application/json"
           :description "'search-term' works with at least one character, considers:\n
- manufacturer
- product
\nEXCLUDES manufacturers
- .. starting with space
- .. with empty string
\nHINT
- 'in-detail'-option works for models with set 'search-term' only\n"
           :coercion reitit.coercion.schema/coercion
           :middleware [accept-json-middleware]
           :swagger {:produces ["application/json"]}
           :handler get-manufacturer-handler
           :parameters {:query {(s/optional-key :type) (s/enum "Software" "Model")
                                (s/optional-key :search-term) s/Str
                                (s/optional-key :in-detail) (s/enum "true" "false")}}
           :responses {200 {:description "OK"
                            :body [(s/conditional
                                    map? {:id s/Uuid
                                          :manufacturer s/Str
                                          :product s/Str
                                          :version (s/maybe s/Str)
                                          :model_id s/Uuid}
                                    string? s/Str)]}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]

   ["models-compatibles"
    {:get {:conflicting true
           :summary "[fe]"
           :accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :middleware [accept-json-middleware]
           :swagger {:produces ["application/json"]}

           :parameters {:query {(s/optional-key :page) s/Int
                                (s/optional-key :size) s/Int
                                ;(s/optional-key :sort_by) (s/enum :manufacturer-asc :manufacturer-desc :product-asc :product-desc)
                                ;(s/optional-key :filter_manufacturer) s/Str
                                ;(s/optional-key :filter_product) s/Str
                                }}
           :handler get-models-compatible-handler
           :responses {200 {:description "OK"
                            :body compatible-response}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]

   ["models-compatibles/:model_id"
    {:get {:conflicting true
           :summary "[fe]"
           :accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :middleware [accept-json-middleware]
           :swagger {:produces ["application/json"]}
           :parameters {:path {:model_id s/Uuid}}
           :handler get-models-compatible-handler
           :responses {200 {:description "OK"
                            :body s/Any}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]

   ;; /inventory/models/*
   ["models"

    ["/:model_id"


     ["/images"
      ["" {:post {:accept "application/json"
                  :summary "Create image [fe]"
                  :description (str "- Limitations: " (config-get :api :images :max-size-mb) " MB\n"
                                    "- Allowed File types: " (str/join ", " (config-get :api :images :allowed-file-types)) "\n"
                                    "- Creates automatically a thumbnail (" (config-get :api :images :thumbnail :width-px)
                                    "px x " (config-get :api :images :thumbnail :height-px) "px)\n")
                  :swagger {:consumes ["application/json"]
                            :produces "application/json"}
                  :coercion reitit.coercion.schema/coercion
                  :middleware [accept-json-middleware]
                  :parameters {:path {:model_id s/Uuid}
                               :header {:x-filename s/Str}}
                  :handler upload-image
                  :responses {200 {:description "OK" :body s/Any}
                              404 {:description "Not Found"}
                              411 {:description "Length Required"}
                              413 {:description "Payload Too Large"}
                              500 {:description "Internal Server Error"}}}}]

      ["/:image_id"
       {:delete {:accept "application/json"
                 :summary "Delete image [fe]"
                 :coercion reitit.coercion.schema/coercion
                 :parameters {:path {:model_id s/Uuid
                                     :image_id s/Uuid}}
                 :handler delete-image
                 :responses {200 {:description "OK"}
                             404 {:description "Not Found"}
                             500 {:description "Internal Server Error"}}}}]]
     ;; inventory/models/m-id/attachments
     ["/attachments"
      [""
       {:get {:accept "application/json"
              :coercion reitit.coercion.schema/coercion
              :middleware [accept-json-middleware]
              :swagger {:produces ["application/json"]}
              :parameters {:path {:model_id s/Uuid}}
              :handler get-models-of-pool-with-pagination-handler
              :responses {200 {:description "OK"
                               :body s/Any}
                          404 {:description "Not Found"}
                          500 {:description "Internal Server Error"}}}

        :post {:accept "application/json"
               :summary "Create attachment [fe]"
               :description (str "- Limitations: " (config-get :api :attachments :max-size-mb) " MB\n"
                                 "- Allowed File types: " (str/join ", " (config-get :api :attachments :allowed-file-types)))
               :coercion reitit.coercion.schema/coercion
               :middleware [accept-json-middleware]
               :swagger {:produces ["application/json"]}
               :parameters {:path {:model_id s/Uuid}
                            :header {:x-filename s/Str}}
               :handler upload-attachment
               :responses {200 {:description "OK"
                                :body s/Any}
                           400 {:description "Bad Request (Coercion error)"
                                :body s/Any}
                           404 {:description "Not Found"}
                           500 {:description "Internal Server Error"}}}}]

      ["/:attachments_id"
       {:get {:accept "application/json"
              :coercion reitit.coercion.schema/coercion
              :middleware [accept-json-middleware]
              :swagger {:produces ["application/json"]}
              :parameters {:path {:model_id s/Uuid
                                  :attachments_id s/Uuid}}
              :handler get-models-of-pool-handler
              :responses {200 {:description "OK"
                               :body s/Any}
                          404 {:description "Not Found"}
                          500 {:description "Internal Server Error"}}}

        :delete {:accept "application/json"
                 :summary "Delete attachment [fe]"
                 :coercion reitit.coercion.schema/coercion
                 :parameters {:path {:model_id s/Uuid
                                     :attachments_id s/Uuid}}
                 :handler delete-attachments
                 :responses {200 {:description "OK"}
                             404 {:description "Not Found"}
                             500 {:description "Internal Server Error"}}}}]]

     ]]])

(defn get-model-by-pool-route []
  ["/:pool_id"

   {:swagger {:conflicting true
              :tags ["Models by pool"]}}


   ["/model"


    ["/"
     {:post {:accept "application/json"
             :summary "Form-Handler: Create model (JSON) [fe]"
             :description description-model-form
             :coercion spec/coercion
             :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
             :parameters {:path {:pool_id uuid?}
                          :body :model/multipart}
             :handler create-model-handler-by-pool-model-json
             :responses {200 {:description "OK"
                              :body {:data :model-optional-response/inventory-model
                                     :validation any?}}
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}}]

    ["/:model_id"
     [""
      {:get {:accept "application/json"
             :summary "Form-Handler: Fetch model data (JSON) [fe]"
             :coercion spec/coercion
             :parameters {:path {:pool_id uuid?
                                 :model_id uuid?}}
             :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
             :handler create-model-handler-by-pool-form-fetch
             :responses {200 {:description "OK"
                              :body :model-get-put-response/inventory-model}
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}

       :patch {:accept "application/json"
               :summary "Form-Handler: Used to patch model-attributes (JSON) | [fe]"
               :coercion reitit.coercion.schema/coercion
               :description description-model-form
               :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
               :parameters {:path {:pool_id s/Uuid
                                   :model_id s/Uuid}
                            :body {:is_cover (s/maybe s/Uuid)}}
               :handler patch-model-handler
               :responses {200 {:description "OK"
                                :body [{:id s/Uuid
                                        :cover_image_id s/Uuid}]}
                           404 {:description "Not Found"}
                           500 {:description "Internal Server Error"}}}

       :delete {:accept "application/json"
                :summary "Form-Handler: Delete form data (JSON) [fe]"
                :swagger {:consumes ["multipart/form-data"]
                          :produces "application/json"}
                :description description-model-form
                :coercion spec/coercion
                :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
                :parameters {:path {:pool_id uuid?
                                    :model_id uuid?}}
                :handler delete-model-handler-by-pool-json
                :responses {200 {:description "OK"
                                 :body {:deleted_attachments [{:id uuid?
                                                               :model_id uuid?
                                                               :filename string?
                                                               :size number?}]
                                        :deleted_images [any?]
                                        :deleted_model [{:id uuid?
                                                         :product string?
                                                         :manufacturer any?}]}}
                            404 {:description "Not Found"}
                            500 {:description "Internal Server Error"}}}}]
     ["/"
      {:put {:accept "application/json"
             :summary "Form-Handler: Update model data (JSON) [fe]"
             :coercion spec/coercion
             :description description-model-form
             :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
             :parameters {:path {:pool_id uuid?
                                 :model_id uuid?}
                          :body :model/multipart}
             :handler update-model-handler-by-pool-model-json
             :responses {200 {:description "OK"
                              :body {:data :model-optional-response/inventory-model
                                     :validation any?}}
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}}]]]




   ;; Routes for /inventory/<pool-id>/*
   ;; TODO: should be? ["/models/list"
   ["/models"
    [""
     {:get {:accept "application/json"
            :summary "Models-List for table"
            :description "- https://staging.leihs.zhdk.ch/manage/8bd16d45-056d-5590-bc7f-12849f034351/models"
            :coercion reitit.coercion.schema/coercion
            :middleware [accept-json-middleware]
            :swagger {:produces ["application/json" "text/html"]}
            :parameters {:path {:pool_id s/Uuid}
                         :query {(s/optional-key :before_last_check) Date
                                 (s/optional-key :borrowable) s/Bool
                                 (s/optional-key :broken) s/Bool
                                 (s/optional-key :category_id) s/Uuid
                                 (s/optional-key :filter_ids) [s/Uuid]
                                 (s/optional-key :filter_manufacturer) s/Str
                                 (s/optional-key :filter_product) s/Str
                                 (s/optional-key :in_stock) s/Bool
                                 (s/optional-key :incomplete) s/Bool
                                 (s/optional-key :inventory_pool_id) s/Uuid
                                 (s/optional-key :is_deletable) s/Bool
                                 (s/optional-key :owned) s/Bool
                                 (s/optional-key :page) s/Int
                                 (s/optional-key :retired) s/Bool
                                 (s/optional-key :search) s/Str
                                 (s/optional-key :size) s/Int
                                 (s/optional-key :sort_by) (s/enum :manufacturer-asc :manufacturer-desc :product-asc :product-desc)
                                 (s/optional-key :type) (s/enum :model :software :option :package)
                                 (s/optional-key :with_items) s/Bool}}

            :handler get-models-of-pool-with-pagination-handler

            :responses {200 {:description "OK"
                             :body (s/->Either [s/Any mc/models-response-payload])}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}




      }
     ]






    ]])
