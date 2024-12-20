(ns leihs.inventory.server.resources.models.form.items.model-by-pool-form-update
  (:require
   [cheshire.core :as cjson]
   [clojure.data.codec.base64 :as b64]
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [leihs.inventory.server.resources.models.form.license.common :refer [cast-to-uuid-or-nil double-to-numeric-or-nil parse-local-date-or-nil calculate-retired-value remove-empty-or-nil remove-entries-by-keys]]
   [clojure.set :as set]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.models.helper :refer [str-to-bool normalize-model-data normalize-files
                                                           process-attachments parse-json-array]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response]]
   [taoensso.timbre :refer [error]])
  (:import [java.time LocalDateTime]))

(defn prepare-item-data [data item-entry]
  (let [
        ;normalize-data (normalize-model-data data)
        created-ts (LocalDateTime/now)

        db-retired (:retired item-entry)
        request-retired (:retired data)
        retired-value (calculate-retired-value db-retired request-retired)
        p (println ">o> ??? retired-value vs db-retired" retired-value db-retired)
        ;data (if (not= retired-value db-retired)
        ;              (assoc data :retired retired-value)
        ;              data)

        data (assoc data :retired retired-value)
        data (when (= false request-retired) (assoc data :retired_reason nil))


        p (println ">o> retired ??? " (:retired data) (:retired_reason data))



        supplier-id (cast-to-uuid-or-nil (:supplier_id data))


        invoice-date (parse-local-date-or-nil (:invoice_date data))
        price (double-to-numeric-or-nil (:price data))

        data (dissoc data :attachments :attachments-to-delete)
        data (dissoc data :properties (parse-json-array data :properties))


        ]
    ;(assoc normalize-data :updated_at created-ts :is_package (str-to-bool (:is_package normalize-data)))))
    (assoc data :updated_at created-ts :invoice_date invoice-date :price price :supplier_id supplier-id)))

(defn process-deletions [tx ids table key]
  (doseq [id (set ids)]
    (jdbc/execute! tx (-> (sql/delete-from table)
                          (sql/where [:= key (to-uuid id)])
                          sql-format))))

;(defn update-item-handler [request]
(defn update-item-handler [{item-id :item_id model-id :model_id pool-id :pool_id tx :tx request :request item-entry :item-entry}]

    (let [
        model-id (to-uuid (get-in request [:path-params :model_id]))
        item-id (to-uuid (get-in request [:path-params :item_id]))
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        multipart (get-in request [:parameters :multipart])
        tx (:tx request)

        prepared-model-data (prepare-item-data multipart item-entry)



        ;prepared-model-data multipart
        ;prepared-model-data (remove-entries-by-keys prepared-model-data [])
        ;prepared-model-data (remove-empty-or-nil prepared-model-data)

        p (println ">o> >>> prepared-item-data" prepared-model-data)
        ]
    (try
      (let [update-model-query (-> (sql/update [:items :i])
                                   (sql/set prepared-model-data)
                                 ;(sql/join [:models :m] [:= :i.model_id :m.id])
                                   (sql/where [:and [:= :i.model_id model-id] [:= :i.id item-id] ])
                                   (sql/returning :*)
                                   sql-format)
            updated-model (jdbc/execute-one! tx update-model-query)


            p (println ">o> ??? updated-model" updated-model)

            ;attachments (normalize-files request :attachments)
            ;attachments-to-delete (parse-json-array request :attachments-to-delete)
            ]
        ;(process-attachments tx attachments model-id)
        ;(process-deletions tx attachments-to-delete :attachments :id)
        (if updated-model
          (response [updated-model])
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