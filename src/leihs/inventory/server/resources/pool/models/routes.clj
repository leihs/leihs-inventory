(ns leihs.inventory.server.resources.pool.models.routes
  (:require
   [clojure.spec.alpha :as sa]
   [clojure.string :as str]
   [leihs.inventory.server.constants :refer [fe]]

   [leihs.inventory.server.resources.pool.models.coercion :as mc]

   [leihs.inventory.server.resources.pool.models.main :refer [post-resource index-resources]]

   [leihs.inventory.server.resources.pool.models.types :refer [get-response post-response]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.auth.role-auth :refer [permission-by-role-and-pool]]
   [leihs.inventory.server.utils.auth.roles :as roles]
   [leihs.inventory.server.utils.coercion.core :refer [Date]]
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

(defn get-models-route []
  ["/:pool_id"

   {:swagger {:tags [""]}}

   ["/models"
    ["/"
     {:get {:accept "application/json"
            :summary (fe "Inventory list")
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

            :handler index-resources

            :responses {200 {:description "OK"
                             :body get-response}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}

      :post {:accept "application/json"
             :summary (fe "Form-Handler: Create model")
             :description description-model-form
             :coercion spec/coercion
             :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
             :parameters {:path {:pool_id uuid?}
                          :body :model/multipart}
             :handler post-resource
             :responses {200 {:description "OK"
                              :body post-response}
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}}]]])
