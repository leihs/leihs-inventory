(ns leihs.inventory.server.resources.pool.models.form.items.model-by-pool-form-update
  (:require
   [cheshire.core :as cjson]
   [cheshire.core :as jsonc]
   [cheshire.core :refer [generate-string] :rename {generate-string to-json}]
   [clojure.data.codec.base64 :as b64]
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.models.form.license.common :refer [remove-nil-entries cast-to-uuid-or-nil double-to-numeric-or-nil parse-local-date-or-nil calculate-retired-value remove-empty-or-nil remove-entries-by-keys]]
   [leihs.inventory.server.resources.pool.models.helper :refer [str-to-bool normalize-model-data normalize-files
                                                           process-attachments parse-json-map parse-json-array]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response]]
   [taoensso.timbre :refer [error]])
  (:import [java.time LocalDateTime]))

(defn create-validation-response [data validation]
  {:data data :validation validation})

(defn prepare-item-data [data item-entry properties]
  (let [created-ts (LocalDateTime/now)
        db-retired (:retired item-entry)
        request-retired (:retired data)
        retired-value (calculate-retired-value db-retired request-retired)
        data (assoc data :retired retired-value)
        data (if (= false request-retired)
               (assoc data :retired_reason nil)
               data)

        supplier-id (cast-to-uuid-or-nil (:supplier_id data))
        invoice-date (parse-local-date-or-nil (:invoice_date data))
        price (double-to-numeric-or-nil (:price data))
        data (dissoc data :attachments :attachments_to_delete)
        properties [:cast (jsonc/generate-string properties) :jsonb]

        data (assoc data :properties properties)
        data (assoc data :updated_at created-ts :invoice_date invoice-date :price price :supplier_id supplier-id)
        data (remove-nil-entries data [:electrical_power :imei_number :model_id :p4u :reference :project_number :warranty_expiration :quantity_allocations])]
    data))

(defn process-deletions [tx ids table key]
  (doseq [id (set ids)]
    (jdbc/execute! tx (-> (sql/delete-from table)
                          (sql/where [:= key (to-uuid id)])
                          sql-format))))

(defn update-item-handler [{item-id :item_id model-id :model_id pool-id :pool_id tx :tx request :request item-entry :item-entry}]
  (let [multipart (get-in request [:parameters :multipart])
        tx (:tx request)
        model-id-to-update (:model_id multipart)
        multipart (if (= model-id-to-update model-id)
                    (dissoc multipart :model_id)
                    multipart)
        properties (first (parse-json-array request :properties))
        prepared-model-data (prepare-item-data multipart item-entry properties)]

    (try
      (let [update-model-query (-> (sql/update [:items :i])
                                   (sql/set prepared-model-data)
                                   (sql/where [:and [:= :i.model_id model-id] [:= :i.id item-id]])
                                   (sql/returning :*)
                                   sql-format)

            updated-model (jdbc/execute-one! tx update-model-query)
            attachments (normalize-files request :attachments)
            attachments-to-delete (parse-json-array request :attachments_to_delete)]

        (process-attachments tx attachments "item_id" item-id)
        (process-deletions tx attachments-to-delete :attachments :id)

        (if updated-model
          (response (create-validation-response updated-model []))
          (bad-request {:error "Failed to update item"})))
      (catch Exception e
        (error "Failed to update item" (.getMessage e))
        (bad-request {:error "Failed to update item" :details (.getMessage e)})))))

(defn fetch-license-data [tx model-id item-id pool-id]
  (let [query (-> (sql/select :*)
                  (sql/from :items)
                  (sql/where [:= :id item-id] [:= :model_id model-id] [:= :inventory_pool_id pool-id])
                  sql-format)
        res (jdbc/execute-one! tx query)]
    res))

(defn update-items-handler-by-pool-form [request]
  (let [item-id (to-uuid (get-in request [:path-params :item_id]))
        model-id (to-uuid (get-in request [:path-params :model_id]))
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        tx (:tx request)
        res (fetch-license-data tx model-id item-id pool-id)]
    (if res
      (update-item-handler {:item_id item-id :model_id model-id :pool_id pool-id :tx tx :request request :item-entry res})
      (bad-request {:error "Failed to update item" :details "No data found"}))))
