(ns leihs.inventory.server.resources.models.form.license.model-by-pool-form-update
  (:require
   [cheshire.core :as jsonc]
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.models.form.license.common :refer [parse-local-date-or-nil calculate-retired-value int-to-numeric int-to-numeric-or-nil double-to-numeric-or-zero double-to-numeric-or-nil
                                                                        cast-to-nil cast-to-uuid-or-nil fetch-default-room-id remove-empty-or-nil
                                                                        parse-json-array normalize-files file-to-base64 base-filename process-attachments]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]])
  (:import [java.math BigDecimal RoundingMode]
           [java.net URL JarURLConnection]
           (java.time LocalDateTime)
           [java.util UUID]))

(defn process-deletions [tx ids table key]
  (doseq [id ids]
    (jdbc/execute! tx (-> (sql/delete-from table)
                          (sql/where [:= key (to-uuid id)])
                          sql-format))))

(defn generate-license-data [request multipart properties pool-id]
  (try
    (let [now-ts (LocalDateTime/now)
          tx (:tx request)
          invoice-date (parse-local-date-or-nil (:invoice_date multipart))
          price (double-to-numeric-or-nil (:price multipart))
          supplier-id (cast-to-uuid-or-nil (:supplier_id multipart))
          merged-data (merge (dissoc multipart :attachments :properties :retired :price :supplier_id :invoice_date :attachments-to-delete)
                             {:updated_at now-ts
                              :properties [:cast (jsonc/generate-string properties) :jsonb]
                              :inventory_pool_id pool-id
                              :owner_id pool-id
                              :room_id (:room_id (fetch-default-room-id tx))})
          merged-data (assoc merged-data :invoice_date invoice-date :price price :supplier_id supplier-id)]
      merged-data)

    (catch Exception e
      (println "EXCEPTION-DETAIL: " (str "generate-license-data: An error occurred, " e))
      (throw (ex-info (str "generate-license-data: An error occurred, " (.getMessage e))
                      {:function-name 'generate-license-data
                       :original-exception e})))))

(defn- update-license-handler [{item-id :item_id model-id :model_id pool-id :pool_id tx :tx request :request item-entry :item-entry}]
  (let [properties (first (parse-json-array request :properties))
        multipart (get-in request [:parameters :multipart])
        update-data (generate-license-data request multipart properties pool-id)
        now-ts (LocalDateTime/now)
        db-retired (:retired item-entry)
        request-retired (:retired multipart)
        retired-value (calculate-retired-value db-retired request-retired)
        update-data (assoc update-data :retired retired-value)]
    (try
      (let [update-model-query (-> (sql/update :items)
                                   (sql/set update-data)
                                   (sql/where [:= :id item-id])
                                   (sql/returning :*)
                                   sql-format)
            updated-model (jdbc/execute-one! tx update-model-query)
            attachments (normalize-files request :attachments)
            attachments-to-delete (parse-json-array request :attachments-to-delete)
            _ (do
                (process-attachments tx attachments "item_id" (:id updated-model))
                (process-deletions tx attachments-to-delete :attachments :id))
            res (jdbc/execute! tx (-> (sql/select :id :filename :content_type :size)
                                      (sql/from :attachments)
                                      (sql/where [:= :item_id item-id])
                                      sql-format))
            updated-model (assoc updated-model :attachments res)]
        (if updated-model
          (response [updated-model])
          (bad-request {:error "Failed to update model"})))
      (catch Exception e
        (error "Failed to update model" (.getMessage e))
        (bad-request {:error "Failed to update model" :details (.getMessage e)})))))

(defn fetch-license-data [tx model-id item-id pool-id]
  (let [query (-> (sql/select :*)
                  (sql/from :items)
                  (sql/where [:= :id item-id] [:= :model_id model-id] [:= :inventory_pool_id pool-id])
                  sql-format)
        res (jdbc/execute-one! tx query)]
    res))

(defn update-license-handler-by-pool-form [request]
  (let [item-id (to-uuid (get-in request [:path-params :item_id]))
        model-id (to-uuid (get-in request [:path-params :model_id]))
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        tx (:tx request)
        res (fetch-license-data tx model-id item-id pool-id)]
    (if res
      (update-license-handler {:item_id item-id :model_id model-id :pool_id pool-id :tx tx :request request :item-entry res})
      (bad-request {:error "Failed to update model" :details "No data found"}))))