(ns leihs.inventory.server.resources.models.form.package.model-by-pool-form-update
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
   [leihs.inventory.server.resources.models.form.license.common :refer [remove-nil-entries cast-to-uuid-or-nil double-to-numeric-or-nil parse-local-date-or-nil calculate-retired-value remove-empty-or-nil remove-entries-by-keys]]
   [leihs.inventory.server.resources.models.form.package.model-by-pool-form-create :refer [prepare-package-data split-items]]
   [leihs.inventory.server.resources.models.helper :refer [str-to-bool normalize-model-data normalize-files
                                                           ;process-attachments-by
                                                           process-attachments parse-json-map parse-json-array]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response]]
   [taoensso.timbre :refer [error]])
  (:import [java.time LocalDateTime]))

;(defn remove-nil-entries
;  "Removes entries from the map if the values of the specified keys are nil."
;  [data keys-to-check]
;  (reduce (fn [m k]
;            (if (nil? (get m k))
;              (dissoc m k)
;              m))
;    data
;    keys-to-check))

(defn create-validation-response [data validation]
  {:data data :validation validation})

(defn prepare-item-data [data item-entry properties]
  (let [;normalize-data (normalize-model-data data)
        created-ts (LocalDateTime/now)

        db-retired (:retired item-entry)
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

        supplier-id (cast-to-uuid-or-nil (:supplier_id data))

        invoice-date (parse-local-date-or-nil (:invoice_date data))
        price (double-to-numeric-or-nil (:price data))

        data (dissoc data :attachments :attachments-to-delete)

        properties [:cast (jsonc/generate-string properties) :jsonb]

        ;;properties (parse-json-map data :properties)
        ;properties ( :properties data)

        ;data (assoc data :properties (to-json properties))
        data (assoc data :properties properties)

        data (assoc data :updated_at created-ts :invoice_date invoice-date :price price :supplier_id supplier-id)

        data (remove-nil-entries data [:electrical_power :imei_number :model_id :p4u :reference :project_number :warranty_expiration :quantity_allocations])]
    data))

(defn process-deletions [tx ids table key]
  (doseq [id (set ids)]
    (jdbc/execute! tx (-> (sql/delete-from table)
                          (sql/where [:= key (to-uuid id)])
                          sql-format))))

;(defn update-item-handler [request]
(defn update-item-handler [{item-id :item_id model-id :model_id pool-id :pool_id tx :tx request :request item-entry :item-entry}]

  (let [;model-id (to-uuid (get-in request [:path-params :model_id]))
        ;item-id (to-uuid (get-in request [:path-params :item_id]))
        ;pool-id (to-uuid (get-in request [:path-params :pool_id]))
        ;multipart (get-in request [:parameters :multipart])
        ;p (println ">o> >>> multipart" multipart)
        ;
        ;tx (:tx request)
        ;
        ;
        ;  model-id-to-update (:model_id multipart)
        ;
        ;
        ;  p (println ">o> ??? model-id-to-update vs model-id" model-id-to-update model-id)
        ;
        ;  multipart (if (= model-id-to-update model-id)
        ;              (dissoc multipart :model_id)
        ;              multipart)
        ;
        ;
        ;
        ;  properties (first (parse-json-array request :properties))
        ;
        ;prepared-model-data (prepare-item-data multipart item-entry properties)

;prepared-model-data multipart
        ;prepared-model-data (remove-entries-by-keys prepared-model-data [])
        ;prepared-model-data (remove-empty-or-nil prepared-model-data)

        created-ts (LocalDateTime/now)
        tx (:tx request)
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        multipart (get-in request [:parameters :multipart])

        items_attributes (parse-json-array request :items_attributes)

        p (println ">o> items_attributes" items_attributes)

        multipart (assoc multipart :inventory_pool_id pool-id)

          ;; FIXME: handle retired_reason with NEW-FE
        multipart (dissoc multipart :retired)

        prepared-package-data (prepare-package-data multipart)

        p (println ">o> prepared-package-data" prepared-package-data)

        split-items (split-items items_attributes)
        p (println ">o> abc.split-items" split-items)

          ;retired: true
          ;retired_reason: jo is retired

          ;>o> retired ???  #object[java.time.LocalDate 0x1389ac10 2025-01-31] jo is retired
        ]
    (try
      (let [update-model-query (-> (sql/update [:items :i])
                                   (sql/set prepared-package-data)
                                 ;(sql/join [:models :m] [:= :i.model_id :m.id])
                                 ;  (sql/where [:= :i.id item-id] )
                                   (sql/where [:and [:= :i.model_id model-id] [:= :i.id item-id]])
                                   (sql/returning :*)
                                   sql-format)
            res (jdbc/execute-one! tx update-model-query)

            p (println ">o> ??? updated-model" res)

;; Link items from package
            link-res (let [ids-to-link (get split-items :ids-to-link)]
                       (when (seq ids-to-link) ;; Ensure ids-to-link is not empty
                         (println ">o> abc.ids-to-link" ids-to-link)

                         (let [update-link-items-query (-> (sql/update :items)
                                                           (sql/set {:parent_id (:id res)})
                                                           (sql/where [:in :id ids-to-link])
                                                           (sql/where [:is :parent_id nil])
                                                           (sql/returning :*)
                                                           sql-format)

                               _ (println ">o> abc.update-link-items-query" update-link-items-query)

                               linked-items-res (jdbc/execute! tx update-link-items-query)]

                           (println ">o> abc.linked-items-res" linked-items-res)
                           linked-items-res)))
            ;)
            ;
            ;; Unlink items from package
            unlink-res (let [ids-to-unlink (get split-items :ids-to-unlink)]
                         (when (seq ids-to-unlink) ;; Ensure ids-to-unlink is not empty
                           (println ">o> ???abc.ids-to-unlink" ids-to-unlink)

                           (let [update-unlink-items-query (-> (sql/update :items)
                                                               (sql/set {:parent_id nil})
                                                               (sql/where [:in :id ids-to-unlink])
                                                               (sql/where [:is-not :parent_id nil])
                                                               (sql/returning :*)
                                                               sql-format)
                                 unlinked-items-res (jdbc/execute! tx update-unlink-items-query)]

                             (println ">o> abc.unlinked" unlinked-items-res)
                             unlinked-items-res)))

            ;attachments (normalize-files request :attachments)
            ;attachments-to-delete (parse-json-array request :attachments-to-delete)
            ]
        ;(process-attachments tx attachments model-id)
        ;(process-attachments tx attachments "item_id" item-id)
        ;(process-deletions tx attachments-to-delete :attachments :id)
        (if res
          ;(response [updated-model])
          (response (create-validation-response res []))
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

(defn update-package-handler-by-pool-form [request]
  (println ">o> update-package-handler-by-pool-form")
  (let [item-id (to-uuid (get-in request [:path-params :item_id]))
        model-id (to-uuid (get-in request [:path-params :model_id]))
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        tx (:tx request)
        res (fetch-license-data tx model-id item-id pool-id)]
    (if res
      (update-item-handler {:item_id item-id :model_id model-id :pool_id pool-id :tx tx :request request :item-entry res})
      (bad-request {:error "Failed to update item" :details "No data found"}))))