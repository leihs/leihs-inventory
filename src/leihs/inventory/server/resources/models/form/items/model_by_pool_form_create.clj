(ns leihs.inventory.server.resources.models.form.items.model-by-pool-form-create
  (:require
   [cheshire.core :as cjson]
   [cheshire.core :as jsonc]
   [clojure.data.codec.base64 :as b64]
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.models.form.license.common :refer [remove-nil-entries cast-to-uuid-or-nil double-to-numeric-or-nil parse-local-date-or-nil calculate-retired-value remove-empty-or-nil remove-entries-by-keys]]

   [leihs.inventory.server.resources.models.helper :refer [str-to-bool normalize-model-data parse-json-array normalize-files
                                                           file-to-base64 base-filename process-attachments]]
   [leihs.inventory.server.resources.models.queries :refer [accessories-query attachments-query base-pool-query
                                                            entitlements-query item-query
                                                            model-links-query properties-query]]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.core :refer [single-entity-get-request?]]
   [leihs.inventory.server.utils.helper :refer [convert-map-if-exist]]
   [leihs.inventory.server.utils.pagination :refer [fetch-pagination-params pagination-response create-pagination-response]]
   [next.jdbc :as jdbc]
   [pantomime.extract :as extract]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]])
  (:import [java.net URL JarURLConnection]
           (java.time LocalDateTime)
           [java.util UUID]
           [java.util.jar JarFile]))

(defn prepare-item-data [data properties]
  (let [created-ts (LocalDateTime/now)
        db-retired nil
        request-retired (:retired data)
        retired-value (calculate-retired-value db-retired request-retired)

        data (assoc data :retired retired-value)
        data (if (= false request-retired)
               (assoc data :retired_reason nil)
               data)

        supplier-id (cast-to-uuid-or-nil (:supplier_id data))
        invoice-date (parse-local-date-or-nil (:invoice_date data))
        price (double-to-numeric-or-nil (:price data))
        data (dissoc data :attachments :attachments-to-delete)
        properties [:cast (jsonc/generate-string properties) :jsonb]

        data (assoc data :properties properties)
        data (assoc data :updated_at created-ts
                    :created_at created-ts :invoice_date invoice-date :price price :supplier_id supplier-id)

        data (remove-nil-entries data [:electrical_power :imei_number :room_id :model_id :p4u :reference :project_number :warranty_expiration :quantity_allocations])]
    data))

(defn create-validation-response [data validation]
  {:data data
   :validation validation})

(defn create-items-handler-by-pool-form [request]
  (let [validation-result (atom [])
        created-ts (LocalDateTime/now)
        tx (:tx request)
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        multipart (get-in request [:parameters :multipart])
        properties (first (parse-json-array request :properties))
        multipart (assoc multipart :inventory_pool_id pool-id)
        prepared-model-data (prepare-item-data multipart properties)
        attachments (normalize-files request :attachments)]

    (try
      (let [res (jdbc/execute-one! tx (-> (sql/insert-into :items)
                                          (sql/values [prepared-model-data])
                                          (sql/returning :*)
                                          sql-format))
            item-id (:id res)]

        (process-attachments tx attachments "item_id" item-id)

        (if res
          (response (create-validation-response res @validation-result))
          (bad-request {:error "Failed to create model"})))
      (catch Exception e
        (error "Failed to create model" (.getMessage e))
        (cond
          (str/includes? (.getMessage e) "unique_model_name_idx")
          (-> (response {:status "failure"
                         :message "Model already exists"
                         :detail {:product (:product prepared-model-data)}})
              (status 409))
          (str/includes? (.getMessage e) "insert or update on table \"models_compatibles\"")
          (-> (response {:status "failure"
                         :message "Modification of models_compatibles failed"
                         :detail {:product (:product prepared-model-data)}})
              (status 409))
          :else (bad-request {:error "Failed to create model" :details (.getMessage e)}))))))
