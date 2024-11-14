(ns leihs.inventory.server.resources.models.model-by-pool-form-fetch
  (:require
   [clojure.data.codec.base64 :as b64]
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [honey.sql :as sq]
   [leihs.inventory.server.resources.models.queries :refer [accessories-query attachments-query
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

(defn select-entries
  [tx table columns where-clause]
  (jdbc/execute! tx
    (-> (apply sql/select columns)
      (sql/from table)
      (sql/where where-clause)
      sql-format)))

(defn create-model-handler-by-pool-form-fetch [request]
  (let [created_ts (LocalDateTime/now)
        tx (get-in request [:tx])
        model-id (to-uuid (get-in request [:path-params :model_id]))
        pool-id (to-uuid (get-in request [:path-params :pool_id]))]

    (try
      (let [query (->
                    (sql/select :m.id :m.product :m.manufacturer :m.version :m.type :m.hand_over_note :m.description
                      :m.internal_description :m.technical_detail :m.is_package)
                    (sql/from [:models :m])
                    (sql/where [:= :m.id model-id])
                    sql-format)

            res (jdbc/execute-one! tx query)

            res2 (select-entries tx :attachments [:id :filename :content_type] [:= :model_id model-id])

            res6 (-> (sql/select :m.cover_image_id :i.id :i.filename :i.content_type)
                   (sql/from [:models :m])
                   (sql/right-join [:images :i] [:= :i.target_id :m.id])
                   (sql/where [:and [:= :m.id model-id] [:= :i.thumbnail false]])
                   sql-format)

            res6 (jdbc/execute! tx res6)

            res6 (map (fn [row]
                        (let [url (str "/inventory/images/" (:id row))
                              thumbnail-url (str "/inventory/images/" (:id row) "/thumbnail")]
                          (assoc row :url url :thumbnail-url thumbnail-url)))
                   res6)

            res3 (-> (sql/select :id :a.name [:aip.inventory_pool_id :has_inventory_pool]
                       [(sq/call :not= :aip.inventory_pool_id nil) :has_inventory_pool])
                   (sql/from [:accessories :a])
                   (sql/left-join [:accessories_inventory_pools :aip] [:= :a.id :aip.accessory_id])
                   (sql/where [:= :a.model_id model-id])
                   (sql/order-by :a.name)
                   sql-format)
            res3 (jdbc/execute! tx res3)

            res4 (-> (sql/select :mm.id :mm.product)
                   (sql/from [:models_compatibles :mc])
                   (sql/left-join [:models :m] [:= :mc.model_id :m.id])
                   (sql/left-join [:models :mm] [:= :mc.compatible_id :mm.id])
                   (sql/where [:= :mc.model_id model-id])
                   sql-format)
            res4 (jdbc/execute! tx res4)

            res5 (select-entries tx :properties [:id :key :value] [:= :model_id model-id])

            res7 (-> (sql/select :e.id :e.quantity :e.position :eg.name [:eg.id :group_id])
                   (sql/from [:entitlements :e])
                   (sql/join [:entitlement_groups :eg] [:= :e.entitlement_group_id :eg.id])
                   (sql/where [:= :e.model_id model-id])
                   sql-format)
            res7 (jdbc/execute! tx res7)

            type "Category"
            res8 (-> (sql/select :mg.id :mg.type :mg.name)
                   (sql/from [:model_groups :mg])
                   (sql/left-join [:model_links :ml] [:= :mg.id :ml.model_group_id])
                   (sql/where [:ilike :mg.type (str type)])
                   (sql/where [:= :ml.model_id model-id])
                   (sql/order-by :mg.name)
                   sql-format)
            res8 (jdbc/execute! tx res8)

            res (assoc res :attachments res2 :accessories res3 :compatibles res4 :properties res5
                  :images res6
                  :entitlement_groups res7
                  :categories res8)]

        (if res
          (response [res])
          (bad-request {:error "Failed to create model"})))
      (catch Exception e
        (error "Failed to fetch model" (.getMessage e))
        (bad-request {:error "Failed to fetch model" :details (.getMessage e)})))))
