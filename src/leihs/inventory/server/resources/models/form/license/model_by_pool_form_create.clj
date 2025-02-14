(ns leihs.inventory.server.resources.models.form.license.model-by-pool-form-create
  (:require
   [cheshire.core :as jsonc]
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.models.form.license.common :refer [int-to-numeric int-to-numeric-or-nil double-to-numeric-or-zero double-to-numeric-or-nil

                                                                        cast-to-nil cast-to-uuid-or-nil fetch-default-room-id remove-empty-or-nil
                                                                        parse-json-array normalize-files file-to-base64 base-filename process-attachments]]
   [leihs.inventory.server.resources.models.helper :refer [str-to-bool normalize-model-data normalize-license-data]]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.core :refer [single-entity-get-request?]]
   [leihs.inventory.server.utils.helper :refer [convert-map-if-exist]]
   [leihs.inventory.server.utils.pagination :refer [fetch-pagination-params pagination-response]]
   [next.jdbc :as jdbc]
   [pantomime.extract :as extract]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]])
  (:import [java.math BigDecimal RoundingMode]
           [java.net URL JarURLConnection]
           (java.time LocalDateTime)
           [java.util UUID]
           [java.util.jar JarFile]))

(defn prepare-model-data [data]
  (let [normalize-data (normalize-license-data data)
        created-ts (LocalDateTime/now)]
    (assoc normalize-data :created_at created-ts :updated_at created-ts)))

(defn create-validation-response [data validation]
  {:data data :validation validation})

(defn generate-license-data [request multipart properties pool-id model-id]
  (try
    (let [now-ts (LocalDateTime/now)
          tx (:tx request)
          invoice_date (:invoice_date multipart)
          invoice-date (when (not (empty? invoice_date)) (java.time.LocalDate/parse invoice_date))
          supplier_id (cast-to-uuid-or-nil (:supplier_id multipart))
          price (double-to-numeric-or-nil (:price multipart))
          multipart2 (dissoc multipart :attachments :retired :supplier_id)
          multipart2b {:created_at now-ts
                       :updated_at now-ts
                       :invoice_date invoice-date
                       :price price
                       :retired (if (= (:retired multipart) false) nil (.toLocalDate now-ts))
                       :properties [:cast (jsonc/generate-string properties) :jsonb]
                       :inventory_pool_id pool-id
                       :model_id model-id
                       :room_id (:room_id (fetch-default-room-id tx))}
          merged-data (merge multipart2 multipart2b)]
      (remove-empty-or-nil merged-data))
    (catch Exception e
      (println ">o> EXCEPTION-DETAIL:: " e))))

(defn create-license-handler-by-pool-form [request]
  (let [validation-result (atom [])
        tx (:tx request)
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        model-id (to-uuid (get-in request [:path-params :model_id]))
        multipart (get-in request [:parameters :multipart])
        attachments (normalize-files request :attachments)
        properties (first (parse-json-array request :properties))
        license-data (generate-license-data request multipart properties pool-id model-id)

        ;;TODO: is this logic correct? Is lending_manager allowed to create license?
        license-data (if (nil? (:owner_id multipart))
                       (do
                         (println ">> ToCHECK / WARNING: no owner_id set, default: pool_id=" pool-id)
                         (assoc license-data :owner_id pool-id))
                       license-data)

        model-data (-> (prepare-model-data multipart)
                       (assoc :is_package (str-to-bool (:is_package multipart))))]
    (try
      (let [res (jdbc/execute-one! tx (-> (sql/insert-into :items)
                                          (sql/values [license-data])
                                          (sql/returning :*)
                                          sql-format))
            item-id (:id res)
            all-attachments (process-attachments tx attachments "item_id" item-id)
            res (assoc res :item_id item-id :attachments all-attachments)]
        (if res
          (response (create-validation-response res @validation-result))
          (bad-request {:error "Failed to create model"})))
      (catch Exception e
        (error "Failed to create model" (.getMessage e))
        (cond
          (str/includes? (.getMessage e) "unique_model_name_idx")
          (-> (response {:status "failure"
                         :message "Model already exists"
                         :detail {:product (:product model-data)}})
              (status 409))
          (str/includes? (.getMessage e) "insert or update on table \"models_compatibles\"")
          (-> (response {:status "failure"
                         :message "Modification of models_compatibles failed"
                         :detail {:product (:product model-data)}})
              (status 409))
          :else (bad-request {:error "Failed to create model" :details (.getMessage e)}))))))
