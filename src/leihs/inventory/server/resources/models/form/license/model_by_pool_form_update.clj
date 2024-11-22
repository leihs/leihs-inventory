(ns leihs.inventory.server.resources.models.form.license.model-by-pool-form-update
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
   [leihs.inventory.server.resources.models.form.model.model-by-pool-form-fetch :refer [create-model-handler-by-pool-form-fetch]]
   [leihs.inventory.server.resources.models.helper :refer [str-to-bool normalize-model-data parse-json-array normalize-files
                                                           file-to-base64 base-filename process-attachments]]
   [leihs.inventory.server.resources.models.queries :refer [accessories-query attachments-query base-pool-query
                                                            entitlements-query item-query
                                                            model-links-query properties-query]]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.helper :refer [convert-map-if-exist]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]])
  (:import [java.net URL JarURLConnection]
           (java.time LocalDateTime)
           [java.util UUID]))

(defn process-deletions [tx ids table key]
  (doseq [id ids]
    (jdbc/execute! tx (-> (sql/delete-from table)
                          (sql/where [:= key (to-uuid id)])
                          sql-format))))
(defn generate-license-data [multipart properties pool-id]
  (let [now-ts (LocalDateTime/now)
        merged-data (merge
                     (dissoc multipart :attachments :properties :retired :invoice_date :price :attachments-to-delete)
                     {:updated_at now-ts
                      :properties [:cast (jsonc/generate-string properties) :jsonb]
                      :inventory_pool_id pool-id

                       ;; FIXME: default room/owner
                      :room_id (to-uuid "503870e1-7fe5-44ef-89e7-11f1c40a9e70")
                      :owner_id (to-uuid "8bd16d45-056d-5590-bc7f-12849f034351")})]
    merged-data))

(defn update-license-handler-by-pool-form [request]
  (let [item-id (to-uuid (get-in request [:path-params :item_id]))
        model-id (to-uuid (get-in request [:path-params :model_id]))
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        tx (:tx request)

        properties (first (parse-json-array request :properties))
        multipart (get-in request [:parameters :multipart])
        update-data (generate-license-data multipart properties pool-id)]
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
