(ns leihs.inventory.server.resources.pool.models.model.fetch-model-form
  (:require
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [honey.sql :as sq :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.common :refer [remove-nil-entries-fnc create-image-url]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response status]]
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
  (->> (select-entries tx :attachments [:id :filename] [:= :model_id model-id])
       (map #(assoc % :url (str "/inventory/attachments/" (:id %))))))

(defn fetch-image-attributes [tx model-id]
  (let [query (-> (sql/select
                   :i.id
                   :i.filename
                   [[[:raw "CASE WHEN m.cover_image_id = i.id THEN TRUE ELSE FALSE END"]]
                    :is_cover])
                  (sql/from [:models :m])
                  (sql/right-join [:images :i] [:= :i.target_id :m.id])
                  (sql/where [:and [:= :m.id model-id] [:= :i.thumbnail false]])
                  sql-format)
        images (jdbc/execute! tx query)]
    (map (fn [row]
           (assoc row :url (str "/inventory/images/" (:id row))
                  :thumbnail_url (str "/inventory/images/" (:id row) "/thumbnail")))
         images)))

(defn fetch-accessories [tx model-id]
  (let [query (-> (sql/select :a.id :a.name)
                  (sql/from [:accessories :a])
                  (sql/left-join [:accessories_inventory_pools :aip] [:= :a.id :aip.accessory_id])
                  (sql/where [:= :a.model_id model-id])
                  (sql/order-by :a.name)
                  sql-format)]
    (jdbc/execute! tx query)))

(defn fetch-compatibles [tx model-id]
  (let [query (-> (sql/select :mm.id :mm.product :mm.version
                              (create-image-url :mm :cover_image_url)
                              :mm.cover_image_id)
                  (sql/from [:models_compatibles :mc])
                  (sql/left-join [:models :m] [:= :mc.model_id :m.id])
                  (sql/left-join [:models :mm] [:= :mc.compatible_id :mm.id])
                  (sql/left-join [:images :i] [:= :mm.cover_image_id :i.id])
                  (sql/where [:= :mc.model_id model-id])
                  sql-format)]
    (-> (jdbc/execute! tx query) remove-nil-entries-fnc)))

(defn fetch-properties [tx model-id]
  (select-entries tx :properties [:id :key :value] [:= :model_id model-id]))

(defn fetch-entitlements [tx model-id]
  (let [query (-> (sql/select :e.id :e.quantity :eg.name [:eg.id :group_id])
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

(defn create-model-handler-by-pool-form-fetch [request]
  (let [current-timestamp (LocalDateTime/now)
        tx (get-in request [:tx])
        model-id (to-uuid (get-in request [:path-params :model_id]))
        pool-id (to-uuid (get-in request [:path-params :pool_id]))]
    (try
      (let [model-query (-> (sql/select :m.id :m.product :m.manufacturer :m.version :m.type
                                        :m.hand_over_note :m.description :m.internal_description
                                        :m.technical_detail :m.is_package)
                            (sql/from [:models :m])
                            (sql/where [:= :m.id model-id])
                            sql-format)
            model-result (jdbc/execute-one! tx model-query)

            attachments (fetch-attachments tx model-id)
            image-attributes (fetch-image-attributes tx model-id)
            accessories (fetch-accessories tx model-id)
            compatibles (fetch-compatibles tx model-id)
            properties (fetch-properties tx model-id)
            entitlements (fetch-entitlements tx model-id)
            categories (fetch-categories tx model-id)
            result (if model-result
                     (assoc model-result
                            :attachments attachments
                            :accessories accessories
                            :compatibles compatibles
                            :properties properties
                            :images image-attributes
                            :entitlements entitlements
                            :categories categories)
                     nil)]
        (if result
          (response result)
          (status
           (response {:status "failure" :message "No entry found"}) 404)))
      (catch Exception e
        (error "Failed to fetch model" e)
        (bad-request {:error "Failed to fetch model" :details (.getMessage e)})))))
