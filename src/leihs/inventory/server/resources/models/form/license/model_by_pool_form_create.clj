(ns leihs.inventory.server.resources.models.form.license.model-by-pool-form-create
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
   [leihs.inventory.server.resources.models.helper :refer [str-to-bool normalize-model-data parse-json-array normalize-files normalize-license-data
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

(defn prepare-model-data
  [data]
  (let [normalize-data (normalize-license-data data)
        created-ts (LocalDateTime/now)]
    (assoc normalize-data
           ;:type "Software"
           :created_at created-ts
           :updated_at created-ts)))

(defn create-validation-response [data validation]
  {:data data
   :validation validation})

(defn remove-empty-or-nil
  "Removes all entries from the map where the value is either nil or an empty string."
  [m]
  (into {}
        (filter (fn [[_ v]] (not (or (nil? v) (= v ""))))
                m)))

(defn generate-license-data [multipart properties pool-id model-id]
  (let [now-ts (LocalDateTime/now)
        multipart2 (dissoc multipart :attachments :retired)
        multipart2b {:created_at now-ts
                     :updated_at now-ts
                     :retired (if (= (:retired multipart) false)
                                nil
                                (.toLocalDate now-ts))
                     :properties [:cast (jsonc/generate-string properties) :jsonb]
                     :inventory_pool_id pool-id
                     :model_id model-id
                     ;; FIXME
                     :room_id (to-uuid "503870e1-7fe5-44ef-89e7-11f1c40a9e70")}
        merged-data (merge multipart2 multipart2b)]
    (remove-empty-or-nil merged-data)))

(defn create-license-handler-by-pool-form [request]
  (let [validation-result (atom [])
        tx (:tx request)

        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        model-id (to-uuid (get-in request [:path-params :model_id]))

        multipart (get-in request [:parameters :multipart])
        attachments (normalize-files request :attachments)
        properties (first (parse-json-array request :properties))
        license-data (generate-license-data multipart properties pool-id model-id)
        model-data (-> (prepare-model-data multipart)
                       (assoc :is_package (str-to-bool (:is_package multipart))))]

    (try
      (let [res (jdbc/execute-one! tx (-> (sql/insert-into :items)
                                          (sql/values [license-data])
                                          (sql/returning :*)
                                          sql-format))
            item-id (:id res)
            all_attachments (process-attachments tx attachments "item_id" item-id)
            res (assoc res :item_id item-id :attachments all_attachments)]

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
