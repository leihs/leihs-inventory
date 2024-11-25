(ns leihs.inventory.server.resources.models.form.license.model-by-pool-form-fetch
  (:require
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [honey.sql :as sq :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
      [leihs.inventory.server.resources.models.queries :refer [accessories-query attachments-query
                                                            entitlements-query item-query
                                                            model-links-query properties-query]]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.core :refer [single-entity-get-request?]]
   [leihs.inventory.server.utils.helper :refer [convert-map-if-exist]]
   [leihs.inventory.server.utils.pagination :refer [fetch-pagination-params pagination-response create-pagination-response]]
   [next.jdbc :as jdbc]

   [next.jdbc.sql :as jdbco]


   [ring.util.response :refer [bad-request response]]
   [taoensso.timbre :refer [error]])
  (:import [java.time LocalDateTime]
           [java.util UUID]))

(defn select-entries [tx table columns where-clause]
  (jdbc/execute! tx
                 (-> (apply sql/select columns)
                     (sql/from table)
                     (sql/where where-clause)
                     sql-format)))

(defn fetch-attachments [tx model-id]
  (select-entries tx :attachments [:id :filename :content_type] [:= :model_id model-id]))

(defn fetch-images [tx model-id]
  (let [query (-> (sql/select :m.cover_image_id :i.id :i.filename :i.content_type)
                  (sql/from [:models :m])
                  (sql/right-join [:images :i] [:= :i.target_id :m.id])
                  (sql/where [:and [:= :m.id model-id] [:= :i.thumbnail false]])
                  sql-format)
        images (jdbc/execute! tx query)]
    (map (fn [row]
           (assoc row :url (str "/inventory/images/" (:id row))
                  :thumbnail-url (str "/inventory/images/" (:id row) "/thumbnail")))
         images)))

(defn fetch-accessories [tx model-id]
  (let [query (-> (sql/select :a.id :a.name [:aip.inventory_pool_id :has_inventory_pool]
                              [(sq/call :not= :aip.inventory_pool_id nil) :has_inventory_pool])
                  (sql/from [:accessories :a])
                  (sql/left-join [:accessories_inventory_pools :aip] [:= :a.id :aip.accessory_id])
                  (sql/where [:= :a.model_id model-id])
                  (sql/order-by :a.name)
                  sql-format)]
    (jdbc/execute! tx query)))

(defn fetch-compatibles [tx model-id]
  (let [query (-> (sql/select :mm.id :mm.product)
                  (sql/from [:models_compatibles :mc])
                  (sql/left-join [:models :m] [:= :mc.model_id :m.id])
                  (sql/left-join [:models :mm] [:= :mc.compatible_id :mm.id])
                  (sql/where [:= :mc.model_id model-id])
                  sql-format)]
    (jdbc/execute! tx query)))

(defn fetch-properties [tx model-id]
  (select-entries tx :properties [:id :key :value] [:= :model_id model-id]))

(defn fetch-entitlements [tx model-id]
  (let [query (-> (sql/select :e.id :e.quantity :e.position :eg.name [:eg.id :group_id])
                  (sql/from [:entitlements :e])
                  (sql/join [:entitlement_groups :eg] [:= :e.entitlement_group_id :eg.id])
                  (sql/where [:= :e.model_id model-id])
                  sql-format)]
    (jdbc/execute! tx query)))

(defn fetch-categories [tx model-id]
  (let [category-type "Category"
        query (-> (sql/select :mg.id :mg.type :mg.name)
                  (sql/from [:model_groups :mg])
                  (sql/left-join [:model_links :ml] [:= :mg.id :ml.model_group_id])
                  (sql/where [:ilike :mg.type (str category-type)])
                  (sql/where [:= :ml.model_id model-id])
                  (sql/order-by :mg.name)
                  sql-format)]
    (jdbc/execute! tx query)))

(defn create-license-handler-by-pool-form-fetch [request]
  (let [current-timestamp (LocalDateTime/now)
        tx (get-in request [:tx])
        model-id (to-uuid (get-in request [:path-params :model_id]))
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        pool_id pool-id

        p (println ">o> params => " pool-id model-id)
        ]
    (try
      (let [
            ;model-query (-> (sql/select :m.id :m.product :m.manufacturer :m.version :m.type
            ;                            :m.hand_over_note :m.description :m.internal_description
            ;                            :m.technical_detail :m.is_package)
            ;                (sql/from [:models :m])
            ;              (sql/join [:items :i] [:= :m.id :i.model_id])
            ;                (sql/where [:= :m.id model-id])
            ;                sql-format)
            ;model-result (jdbc/execute-one! tx model-query)


            model-result (if model-id
                           (let [
                                 model-query (-> (sql/select :m.id :m.product :m.manufacturer :m.version :m.type
                                                   :m.hand_over_note :m.description :m.internal_description
                                                   :m.technical_detail :m.is_package :i.*)
                                               (sql/from [:models :m])
                                               (sql/join [:items :i] [:= :m.id :i.model_id])
                                               (sql/where [:= :m.id model-id])
                                               sql-format)
                                 model-result (jdbc/execute-one! tx model-query)
                                 ]
                             model-result

                                        )
                           {})


            p (println ">o> model-result" model-result)


            query (-> (sql/select :f.id
                  :f.active
                  :f.position
                  :f.data
                  :f.dynamic
                  [(sq/call :cast
                     (sq/call :jsonb_extract_path_text :f.data "permissions" "owner")
                     :text) :owner]
                  [(sq/call :cast
                     (sq/call :jsonb_extract_path_text :f.data "group")
                     :text) :group])
              (sql/from [:fields :f])
              (sql/where [:= :f.active true])
              (sql/where [:or
                          [:in (sq/call :jsonb_extract_path_text :f.data "group")
                           ["Status" "Invoice Information" "General Information" "Inventory" "Maintenance"]]
                          [:is (sq/call :jsonb_extract_path_text :f.data "group") nil]])


;; TODO: additional exclude of fields
                    ;(cond-> id (sql/where [:not-in :f.id ["is_incomplete" "is_broken"]]))
                    (sql/where [:not-in :f.id ["is_incomplete" "is_broken" "status_note" "model_id"]])


                    (sql/order-by [(sq/call :jsonb_extract_path_text :f.data "group") :asc]
                [:f.position :asc])
                    sql-format
                    )

            p (println ">o> query" query)

            fields-result (jdbc/execute! tx query)

            p (println ">o> fields >> " fields-result)

            fields fields-result

            result (if model-result
                     {
                      :data model-result
                      :fields fields
                      }
                     {}
                     )]
        (if result
          (response result)
          (bad-request {:error "Failed to fetch license"})))
      (catch Exception e
        (error "Failed to fetch license" (.getMessage e))
        (bad-request {:error "Failed to fetch license" :details (.getMessage e)})))))
