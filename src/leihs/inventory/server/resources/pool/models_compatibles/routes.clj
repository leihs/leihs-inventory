(ns leihs.inventory.server.resources.pool.models-compatibles.routes
  (:require
   [clojure.spec.alpha :as sa]
   [clojure.string :as str]
   ;[leihs.inventory.server.resources.pool.accessories.main :refer [get-accessories-of-pool-handler]]
   ;[leihs.inventory.server.resources.pool.attachments.main :refer [delete-attachments]]
   [leihs.inventory.server.resources.pool.models.coercion :as mc]
   ;[leihs.inventory.server.resources.pool.models.entitlements.main :refer [get-entitlements-with-pagination-handler]]
   ;[leihs.inventory.server.resources.pool.models.form.items.model-by-pool-form-create :refer [create-items-handler-by-pool-form]]
   ;[leihs.inventory.server.resources.pool.models.form.items.model-by-pool-form-fetch :refer [fetch-items-handler-by-pool-form]]
   ;[leihs.inventory.server.resources.pool.models.form.items.model-by-pool-form-update :refer [update-items-handler-by-pool-form]]
   ;[leihs.inventory.server.resources.pool.models.form.license.model-by-pool-form-create :refer [create-license-handler-by-pool-form]]
   ;[leihs.inventory.server.resources.pool.models.form.license.model-by-pool-form-fetch :refer [fetch-license-handler-by-pool-form-fetch]]
   ;[leihs.inventory.server.resources.pool.models.form.license.model-by-pool-form-update :refer [update-license-handler-by-pool-form]]
   ;[leihs.inventory.server.resources.pool.models.form.model.common :refer [delete-image
   ;                                                                   patch-model-handler
   ;                                                                   patch-models-handler
   ;                                                                   upload-attachment
   ;                                                                   upload-image]]
   ;[leihs.inventory.server.resources.pool.models.form.model.model-by-pool-json-create :refer [create-model-handler-by-pool-model-json]]
   ;[leihs.inventory.server.resources.pool.models.form.model.model-by-pool-json-delete :refer [delete-model-handler-by-pool-json]]
   ;[leihs.inventory.server.resources.pool.models.form.model.model-by-pool-json-fetch :refer [create-model-handler-by-pool-form-fetch]]
   ;[leihs.inventory.server.resources.pool.models.form.model.model-by-pool-json-update :refer [update-model-handler-by-pool-model-json]]
   ;[leihs.inventory.server.resources.pool.models.form.option.model-by-pool-form-create :refer [create-option-handler-by-pool-form]]
   ;[leihs.inventory.server.resources.pool.models.form.option.model-by-pool-form-fetch :refer [fetch-option-handler-by-pool-form]]
   ;[leihs.inventory.server.resources.pool.models.form.option.model-by-pool-form-update :refer [update-option-handler-by-pool-form]]
   ;[leihs.inventory.server.resources.pool.models.form.package.model-by-pool-form-create :refer [create-package-handler-by-pool-form]]
   ;[leihs.inventory.server.resources.pool.models.form.package.model-by-pool-form-fetch :refer [fetch-package-handler-by-pool-form]]
   ;[leihs.inventory.server.resources.pool.models.form.package.model-by-pool-form-update :refer [update-package-handler-by-pool-form]]
   ;[leihs.inventory.server.resources.pool.models.form.software.model-by-pool-form-create :refer [create-software-handler-by-pool-form]]
   ;[leihs.inventory.server.resources.pool.models.form.software.model-by-pool-form-fetch :refer [create-software-handler-by-pool-form-fetch]]
   ;[leihs.inventory.server.resources.pool.models.form.software.model-by-pool-form-update :refer [delete-software-handler-by-pool-form
   ;                                                                                         update-software-handler-by-pool-form]]
   ;[leihs.inventory.server.resources.pool.models.inventory-list :refer [inventory-list-handler]]
   ;[leihs.inventory.server.resources.pool.models.items.main :refer [get-items-with-pagination-handler]]
   [leihs.inventory.server.resources.pool.models-compatibles.main :refer [
                                                                          ;create-model-handler
                                                         ;delete-model-handler
                                                         ;get-manufacturer-handler
                                                         get-models-compatible-handler
                                                         ;update-model-handler
                                                                          ]]
   ;[leihs.inventory.server.resources.pool.models.model-links.main :refer [get-model-links-with-pagination-handler]]
   ;[leihs.inventory.server.resources.pool.models.models-by-pool :refer [create-model-handler-by-pool
   ;                                                                delete-model-handler-by-pool
   ;                                                                get-models-handler
   ;                                                                get-models-of-pool-auto-pagination-handler
   ;                                                                get-models-of-pool-handler
   ;                                                                get-models-of-pool-with-pagination-handler
   ;                                                                update-model-handler-by-pool]]
   ;[leihs.inventory.server.resources.pool.models.properties.main :refer [get-properties-with-pagination-handler]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.auth.role-auth :refer [permission-by-role-and-pool]]
   [leihs.inventory.server.utils.auth.roles :as roles]
   [leihs.inventory.server.utils.coercion.core :refer [Date]]
   [leihs.inventory.server.utils.constants :refer [config-get]]
   [reitit.coercion.schema]
   [reitit.coercion.spec :as spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(def compatible-data {(s/optional-key :cover_image_id) s/Uuid
                      (s/optional-key :cover_image_url) s/Str
                      :model_id s/Any
                      :product s/Str
                      (s/optional-key :version) s/Str})

(def compatible-response
  (s/->Either [[compatible-data] {:data [compatible-data] :pagination s/Any}]))



(defn get-models-compatibles-route []
  ["/"
   {:swagger {:conflicting true
              :tags ["Models"]}}

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
                       500 {:description "Internal Server Error"}}}}] ])



