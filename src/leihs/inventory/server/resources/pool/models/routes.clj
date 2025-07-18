(ns leihs.inventory.server.resources.pool.models.routes
  (:require
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.models.main :as models]
   [leihs.inventory.server.resources.pool.models.types :refer [description-model-form
                                                               get-response
                                                               post-response]]
   [leihs.inventory.server.utils.auth.role-auth :refer [permission-by-role-and-pool]]
   [leihs.inventory.server.utils.auth.roles :as roles]
   [leihs.inventory.server.utils.coercion.core :refer [Date]]
   [leihs.inventory.server.utils.middleware :refer [accept-json-middleware]]
   [reitit.coercion.schema]
   [reitit.coercion.spec :as spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
    ["/models/"
     {:get {:accept "application/json"
            :summary (fe "Inventory list")
            :description "- https://staging.leihs.zhdk.ch/manage/8bd16d45-056d-5590-bc7f-12849f034351/models"
            :coercion reitit.coercion.schema/coercion
            :middleware [accept-json-middleware]
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

            :handler models/index-resources

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
             :handler models/post-resource
             :responses {200 {:description "OK"
                              :body post-response}
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}}])
