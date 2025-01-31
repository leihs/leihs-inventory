(ns leihs.inventory.server.resources.models.form.package.model-by-pool-form-create
  (:require
   [cheshire.core :as cjson]
   [clojure.data.codec.base64 :as b64]
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.data.json :as json]
   [leihs.inventory.server.utils.helper :refer [convert-map-if-exist]]
   [cheshire.core :as jsonc]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.models.form.license.common :refer [remove-nil-entries cast-to-uuid-or-nil double-to-numeric-or-nil parse-local-date-or-nil calculate-retired-value remove-empty-entries remove-empty-or-nil remove-entries-by-keys]]

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





(defn prepare-package-data [data]
;(defn prepare-package-data [data items_attributes]
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

        ;data (dissoc data :attachments :attachments-to-delete)

        ;properties [:cast (jsonc/generate-string items_attributes) :jsonb]
        ;data (assoc data :properties properties)



        data (assoc data :updated_at created-ts

               ;; FIXME
               ;:room_id #uuid "95c6329a-214a-4db5-8fd3-0b9ccf02705b"

               :created_at created-ts :invoice_date invoice-date :price price
               ;;:supplier_id supplier-id
               )


        p (println ">o> ??? abc:last_check" (:last_check data) (type (:last_check data)))

        p (println ">o> abc1" (type data))
        ;data (remove-nil-entries data [:electrical_power :imei_number :room_id :model_id :p4u :reference :project_number :warranty_expiration :quantity_allocations])
        data (remove-nil-entries data [:retired :last_check :user_name  :shelf :status_note :note])
        p (println ">o> abc1" (type data))
        data (remove-empty-entries data [:retired :last_check :user_name  :shelf :status_note :note])

        p (println ">o> abc2" (type data))
        data (dissoc data :items_attributes)

        p (println ">o> abc3" (type data))
        data (convert-map-if-exist data)
        p (println ">o> abc4" (type data))


        ]
    data
    ))



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
  (println ">o> create-package-handler-by-pool-form" )
  (let [validation-result (atom [])
        created-ts (LocalDateTime/now)
        tx (:tx request)
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        multipart (get-in request [:parameters :multipart])

        items_attributes (parse-json-array request :items_attributes)

        p (println ">o> items_attributes" items_attributes)

        multipart (assoc multipart :inventory_pool_id pool-id)
        prepared-package-data (prepare-package-data multipart)

        p (println ">o> prepared-package-data" prepared-package-data)
        ]

    (try
      (let [




            res (jdbc/execute-one! tx (-> (sql/insert-into :items)
                                          (sql/values [prepared-package-data])
                                          (sql/returning :*)
                                          sql-format))



            split-items (split-items items_attributes)
            p (println ">o> abc.split-items" split-items)

            ;; TODO: link items to package
            ;res-linked (link-items-to-package tx items_attributes res)

            ;;; link items from package
            ;ids (:ids-to-link split-items)
            ;res (when ids
            ;        (let [
            ;          update-link-items-query (-> (sql/update :items)
            ;                             (sql/set {:parent_id (:id res)})
            ;                             (sql/where [:in :id ids] [:is :parent_id nil])
            ;                             (sql/returning :*)
            ;                             sql-format)
            ;          linked-items-res (jdbc/execute! tx update-link-items-query)
            ;              ]
            ;          linked-items-res
            ;          )
            ;      )
            ;p (println ">o> abc.linked" res)
            ;
            ;;; unlink items from package
            ;
            ;ids (:ids-to-link split-items)
            ;res (when ids
            ;        (let [
            ;          update-link-items-query (-> (sql/update :items)
            ;                             (sql/set {:parent_id (:id res)})
            ;                             ;(sql/where [:in :id item-ids])
            ;                             (sql/where [:in :id (:ids-to-unlink split-items)] [:is-not :parent_id nil])
            ;                             (sql/returning :*)
            ;                             sql-format)
            ;          linked-items-res (jdbc/execute! tx update-link-items-query)
            ;
            ;              ]
            ;          linked-items-res
            ;          )
            ;      )
            ;p (println ">o> abc.unlinked" res)



            ;; Link items from package
            link-res (if-let [ids-to-link (get split-items :ids-to-link)]
              ;(when (seq ids-to-link)  ;; Ensure ids-to-link is not empty
                (let [update-link-items-query (-> (sql/update :items)
                                                (sql/set {:parent_id (:id res)})
                                                (sql/where [:in :id ids-to-link])
                                                (sql/where [:is :parent_id nil])
                                                (sql/returning :*)
                                                sql-format)
                      linked-items-res (jdbc/execute! tx update-link-items-query)]
                  (println ">o> abc.linked" linked-items-res)
                  linked-items-res))
              ;)
            ;
            ;; Unlink items from package
            unlink-res (if-let [ids-to-unlink (get split-items :ids-to-unlink)]
              ;(when (seq ids-to-unlink)  ;; Ensure ids-to-unlink is not empty
                (let [update-unlink-items-query (-> (sql/update :items)
                                                  (sql/set {:parent_id nil})
                                                  (sql/where [:in :id ids-to-unlink])
                                                  (sql/where [:is-not :parent_id nil])
                                                  (sql/returning :*)
                                                  sql-format)
                      unlinked-items-res (jdbc/execute! tx update-unlink-items-query)]
                  (println ">o> abc.unlinked" unlinked-items-res)
                  unlinked-items-res))
        ;)



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
                         :detail {:product (:product prepared-package-data)}})
              (status 409))
          (str/includes? (.getMessage e) "insert or update on table \"models_compatibles\"")
          (-> (response {:status "failure"
                         :message "Modification of models_compatibles failed"
                         :detail {:product (:product prepared-package-data)}})
              (status 409))
          :else (bad-request {:error "Failed to create model" :details (.getMessage e)}))))))
