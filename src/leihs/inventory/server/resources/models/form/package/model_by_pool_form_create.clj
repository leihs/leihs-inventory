(ns leihs.inventory.server.resources.models.form.package.model-by-pool-form-create
  (:require
   [cheshire.core :as cjson]
   [clojure.data.codec.base64 :as b64]
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.data.json :as json]
   [cheshire.core :as jsonc]
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

;(defn prepare-software-data
;  [data]
;  (let [normalize-data (normalize-model-data data)
;        created-ts (LocalDateTime/now)]
;    (assoc normalize-data
;           :type "Software"
;           :created_at created-ts
;           :updated_at created-ts)))



;(defn prepare-item-data [data item-entry properties]
;  (let [
;        ;normalize-data (normalize-model-data data)
;        created-ts (LocalDateTime/now)
;
;        db-retired (:retired item-entry)
;        request-retired (:retired data)
;        retired-value (calculate-retired-value db-retired request-retired)
;        p (println ">o> ??? retired-value vs db-retired" retired-value db-retired)
;        ;data (if (not= retired-value db-retired)
;        ;              (assoc data :retired retired-value)
;        ;              data)
;
;        data (assoc data :retired retired-value)
;        data (if (= false request-retired)
;               (assoc data :retired_reason nil)
;               data)
;
;
;        p (println ">o> retired ??? " (:retired data) (:retired_reason data))
;
;
;
;        supplier-id (cast-to-uuid-or-nil (:supplier_id data))
;
;
;        invoice-date (parse-local-date-or-nil (:invoice_date data))
;        price (double-to-numeric-or-nil (:price data))
;
;        data (dissoc data :attachments :attachments-to-delete)
;
;        properties [:cast (jsonc/generate-string properties) :jsonb]
;
;        ;;properties (parse-json-map data :properties)
;        ;properties ( :properties data)
;
;        ;data (assoc data :properties (to-json properties))
;        data (assoc data :properties properties)
;
;        data (assoc data :updated_at created-ts :invoice_date invoice-date :price price :supplier_id supplier-id)
;
;        data (remove-nil-entries data [:electrical_power :imei_number :model_id :p4u :reference :project_number :warranty_expiration :quantity_allocations])
;
;
;        ]
;    data
;    ))





(defn prepare-package-data [data items_attributes]
;(defn prepare-item-data [data item-entry properties]
  (let [
        ;normalize-data (normalize-model-data data)
        created-ts (LocalDateTime/now)

        db-retired nil
        request-retired (:retired data)
        retired-value (calculate-retired-value db-retired request-retired)
        p (println ">o> ??? retired-value vs db-retired" retired-value db-retired)
        ;data (if (not= retired-value db-retired)
        ;              (assoc data :retired retired-value)
        ;              data)

        data (assoc data :retired retired-value)
        data (if (= false request-retired)
               (assoc data :retired_reason nil)
               data)


        p (println ">o> retired ??? " (:retired data) (:retired_reason data))



        ;supplier-id (cast-to-uuid-or-nil (:supplier_id data))


        invoice-date (parse-local-date-or-nil (:invoice_date data))
        price (double-to-numeric-or-nil (:price data))

        data (dissoc data :attachments :attachments-to-delete)

        properties [:cast (jsonc/generate-string items_attributes) :jsonb]

        ;;properties (parse-json-map data :properties)
        ;properties ( :properties data)

        ;data (assoc data :properties (to-json properties))
        data (assoc data :properties properties)

        data (assoc data :updated_at created-ts

               ;; FIXME
               ;:room_id #uuid "95c6329a-214a-4db5-8fd3-0b9ccf02705b"

               :created_at created-ts :invoice_date invoice-date :price price
               ;;:supplier_id supplier-id
               )

        ;data (remove-nil-entries data [:electrical_power :imei_number :room_id :model_id :p4u :reference :project_number :warranty_expiration :quantity_allocations])


        ]
    data
    ))



(defn create-validation-response [data validation]
  {:data data
   :validation validation})

(defn create-package-handler-by-pool-form [request]
  (println ">o> create-package-handler-by-pool-form" )
  (let [validation-result (atom [])
        created-ts (LocalDateTime/now)
        tx (:tx request)
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        multipart (get-in request [:parameters :multipart])

        items_attributes (first (parse-json-array request :items_attributes))

        multipart (assoc multipart :inventory_pool_id pool-id)

        prepared-model-data (prepare-package-data multipart items_attributes)
        ;prepared-model-data (->
        ;                      ;(prepare-software-data multipart)
        ;                      (prepare-item-data multipart properties)
        ;                        (assoc :is_package (str-to-bool (:is_package multipart))))

        ;prepared-model-data (assoc multipart :is_package (str-to-bool (:is_package multipart)))

        ;attachments (normalize-files request :attachments)
        ]

    (try
      (let [res (jdbc/execute-one! tx (-> (sql/insert-into :items)
                                          (sql/values [prepared-model-data])
                                          (sql/returning :*)
                                          sql-format))
            item-id (:id res)]

        ;(process-attachments tx attachments "item_id" item-id)

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
