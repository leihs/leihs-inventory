(ns leihs.inventory.server.resources.pool.models.model.routes
  (:require
   [clojure.spec.alpha :as sa]
   [clojure.string :as str]
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.models.coercion :as mc]
   [leihs.inventory.server.resources.pool.models.model.common-model-form :refer [patch-resource]]
   [leihs.inventory.server.resources.pool.models.model.delete-model-form :refer [delete-resource]]
   [leihs.inventory.server.resources.pool.models.model.fetch-model-form :refer [get-resource]]
   [leihs.inventory.server.resources.pool.models.model.main :refer [update-model-handler
                                                                    delete-model-handler
                                                                    get-models-handler]]
   [leihs.inventory.server.resources.pool.models.model.types :refer [patch-response
                                                                     put-response
                                                                     delete-response]]
   [leihs.inventory.server.resources.pool.models.model.update-model-form :refer [put-resource]]
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

(defn get-models-single-route []
  ["/:pool_id"

   {:swagger {:tags [""]}}

   ["/models"

    ["/:model_id"
     [""
      {:get {:accept "application/json"
             :summary (fe "Form-Handler: Fetch model")
             :coercion spec/coercion
             :parameters {:path {:pool_id uuid?
                                 :model_id uuid?}}
             :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
             :handler get-resource
             :responses {200 {:description "OK"
                              :body :model-get-put-response/inventory-model}
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}

       :patch {:accept "application/json"
               :summary (fe "Form-Handler: Used to patch model-attributes")
               :coercion reitit.coercion.schema/coercion
               :description description-model-form
               :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
               :parameters {:path {:pool_id s/Uuid
                                   :model_id s/Uuid}
                            :body {:is_cover (s/maybe s/Uuid)}}
               :handler patch-resource
               :responses {200 {:description "OK"
                                :body patch-response}
                           404 {:description "Not Found"}
                           500 {:description "Internal Server Error"}}}

       :delete {:accept "application/json"
                :summary (fe "Form-Handler: Delete model")
                :swagger {:consumes ["multipart/form-data"]
                          :produces "application/json"}
                :description description-model-form
                :coercion spec/coercion
                :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
                :parameters {:path {:pool_id uuid?
                                    :model_id uuid?}}
                :handler delete-resource
                :responses {200 {:description "OK"
                                 :body delete-response}
                            404 {:description "Not Found"}
                            500 {:description "Internal Server Error"}}}

       :put {:accept "application/json"
             :summary (fe "Form-Handler: Update model")
             :coercion spec/coercion
             :description description-model-form
             :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
             :parameters {:path {:pool_id uuid?
                                 :model_id uuid?}
                          :body :model/multipart}
             :handler put-resource
             :responses {200 {:description "OK"
                              :body put-response}
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}}]]]])
