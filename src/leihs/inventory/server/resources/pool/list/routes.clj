(ns leihs.inventory.server.resources.pool.list.routes
  (:require
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.list.main :as list]
   [leihs.inventory.server.resources.pool.list.types :refer [get-response]]
   [leihs.inventory.server.utils.auth.role-auth :refer [permission-by-role-and-pool]]
   [leihs.inventory.server.utils.auth.roles :as roles]
   [leihs.inventory.server.utils.coercion.core :refer [Date]]
   [leihs.inventory.server.utils.middleware :refer [accept-json-middleware]]
   [reitit.coercion.schema]
   [reitit.coercion.spec :as spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  ["/list/"
   {:get {:accept "application/json"
          :summary (fe "InventoryList-Endpoint with filters for models, software, options and packages")
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

          :handler list/index-resources
          :responses {200 {:description "OK"
                           :body get-response}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}}])
