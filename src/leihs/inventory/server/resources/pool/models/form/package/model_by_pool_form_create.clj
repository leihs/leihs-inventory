(ns leihs.inventory.server.resources.pool.models.form.package.model-by-pool-form-create
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
   [leihs.inventory.server.resources.pool.models.form.license.common :refer [remove-nil-entries cast-to-uuid-or-nil double-to-numeric-or-nil parse-local-date-or-nil calculate-retired-value remove-empty-entries remove-empty-or-nil remove-entries-by-keys]]
   [leihs.inventory.server.resources.pool.models.helper :refer [str-to-bool normalize-model-data parse-json-array normalize-files
                                                           file-to-base64 base-filename process-attachments]]

   [leihs.inventory.server.resources.pool.models.queries :refer [accessories-query attachments-query base-pool-query
                                                            entitlements-query item-query
                                                            model-links-query properties-query]]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.core :refer [single-entity-get-request?]]
   [leihs.inventory.server.utils.helper :refer [convert-map-if-exist]]
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

(defn prepare-package-data [data]
  (let [created-ts (LocalDateTime/now)
        db-retired nil
        request-retired (:retired data)
        data (if (= false request-retired)
               (assoc data :retired_reason nil)
               data)

        retired-value (calculate-retired-value db-retired request-retired)
        data (assoc data :retired retired-value)
        invoice-date (parse-local-date-or-nil (:invoice_date data))
        price (double-to-numeric-or-nil (:price data))
        data (assoc data :updated_at created-ts
                    :created_at created-ts :invoice_date invoice-date :price price)

        data (remove-nil-entries data [:invoice_date :price :room_id :last_check :user_name :shelf :status_note :note])
        data (remove-empty-entries data [:room_id :last_check :user_name :shelf :status_note :note])
        data (dissoc data :items_attributes)
        data (convert-map-if-exist data)]

    data))

(defn create-validation-response [data validation]
  {:data data
   :validation validation})

(defn split-items
  "Splits items into two vectors:
   - `ids-to-unlink` (items with `:delete true`)
   - `ids-to-link` (items without `:delete` or `false`)."
  [items]
  (reduce
   (fn [{:keys [ids-to-unlink ids-to-link]} item]
     (if (:delete item)
       {:ids-to-unlink (conj ids-to-unlink (to-uuid (:id item)))
        :ids-to-link ids-to-link}
       {:ids-to-unlink ids-to-unlink
        :ids-to-link (conj ids-to-link (to-uuid (:id item)))}))
   {:ids-to-unlink [] :ids-to-link []}
   items))

(defn create-package-handler-by-pool-form [request]
  (let [validation-result (atom [])
        created-ts (LocalDateTime/now)
        tx (:tx request)
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        multipart (get-in request [:parameters :multipart])

        items_attributes (parse-json-array request :items_attributes)
        multipart (assoc multipart :inventory_pool_id pool-id)
        ;;TODO: is this logic correct?
        multipart (if (nil? (:owner_id multipart))
                    (do
                      (println ">> ToCHECK / WARNING: no owner_id set, default: pool_id=" pool-id)
                      (assoc multipart :owner_id pool-id))
                    multipart)

        prepared-package-data (prepare-package-data multipart)]

    (try
      (let [res (jdbc/execute-one! tx (-> (sql/insert-into :items)
                                          (sql/values [prepared-package-data])
                                          (sql/returning :*)
                                          sql-format))

            split-items (split-items items_attributes)

            ;; Link items from package
            link-res (let [ids-to-link (get split-items :ids-to-link)]
                       (when (seq ids-to-link)
                         (let [update-link-items-query (-> (sql/update :items)
                                                           (sql/set {:parent_id (:id res)})
                                                           (sql/where [:in :id ids-to-link])
                                                           (sql/where [:is :parent_id nil])
                                                           (sql/returning :*)
                                                           sql-format)

                               linked-items-res (jdbc/execute! tx update-link-items-query)]
                           linked-items-res)))

            ;; Unlink items from package
            unlink-res (let [ids-to-unlink (get split-items :ids-to-unlink)]
                         (when (seq ids-to-unlink)
                           (let [update-unlink-items-query (-> (sql/update :items)
                                                               (sql/set {:parent_id nil})
                                                               (sql/where [:in :id ids-to-unlink])
                                                               (sql/where [:is-not :parent_id nil])
                                                               (sql/returning :*)
                                                               sql-format)
                                 unlinked-items-res (jdbc/execute! tx update-unlink-items-query)]
                             unlinked-items-res)))
            item-id (:id res)]

        (if res
          (response (create-validation-response res @validation-result))
          (bad-request {:error "Failed to create model"})))
      (catch Exception e
        (error "Failed to create model" (.getMessage e))
        (cond
          (str/includes? (.getMessage e) "unique_model_name_idx")
          (-> (response {:status "failure"
                         :message "Model already exists"
                         :detail {:product (:product prepared-package-data)}})
              (status 409))
          (str/includes? (.getMessage e) "insert or update on table \"models_compatibles\"")
          (-> (response {:status "failure"
                         :message "Modification of models_compatibles failed"
                         :detail {:product (:product prepared-package-data)}})
              (status 409))
          :else (bad-request {:error "Failed to create model" :details (.getMessage e)}))))))
