(ns leihs.inventory.server.resources.pool.models.model.routes
  (:require
   [clojure.spec.alpha :as sa]
   [clojure.string :as str]
   [leihs.inventory.server.resources.pool.attachments.main :refer [delete-attachments]]
   [leihs.inventory.server.resources.pool.models.coercion :as mc]
   [leihs.inventory.server.resources.pool.models.form.model.common :refer [
                                                                           delete-image
                                                                      patch-model-handler
                                                                      patch-models-handler
                                                                      upload-attachment
                                                                      upload-image
    ]]
   [leihs.inventory.server.resources.pool.models.form.model.model-by-pool-json-create :refer [create-model-handler-by-pool-model-json]]
   [leihs.inventory.server.resources.pool.models.form.model.model-by-pool-json-delete :refer [delete-model-handler-by-pool-json]]
   [leihs.inventory.server.resources.pool.models.form.model.model-by-pool-json-fetch :refer [create-model-handler-by-pool-form-fetch]]
   [leihs.inventory.server.resources.pool.models.form.model.model-by-pool-json-update :refer [update-model-handler-by-pool-model-json]]

   [leihs.inventory.server.resources.pool.models.models-by-pool :refer [

                                                                   get-models-of-pool-handler
                                                                   get-models-of-pool-with-pagination-handler
    ]]
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

(defn get-model-single-route []
  ["/"
   {:swagger {:conflicting true
              :tags ["Models"]}}

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

(defn get-models-single-route []
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
                         500 {:description "Internal Server Error"}}}}]]



    ]])
