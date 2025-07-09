(ns leihs.inventory.server.resources.pool.models.model.fetch-model-form
  (:require
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [honey.sql :as sq :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.common :refer [remove-nil-entries-fnc]]

   [leihs.inventory.server.resources.pool.models.common :refer [apply-cover-image-urls create-url fetch-thumbnails-for-ids]]

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

(defn fetch-attachments [tx model-id pool-id]
  (->> (select-entries tx :attachments [:id :filename] [:= :model_id model-id])
       (map #(assoc % :url (str "/inventory/" pool-id "/models/" model-id "/attachments/" (:id %))))))

(defn filtered-cover-ids
  "Given a cover_image_id and a collection of images,
     returns {:main main-id, :thumbnail thumb-id} for the filtered cover images."
  [cover_image_id images]
  (let [filtered-cover (when cover_image_id
                         (filter #(or (= (:id %) cover_image_id)
                                      (= (:parent_id %) cover_image_id))
                                 images))
        main-id (->> filtered-cover
                     (filter #(not (:thumbnail %)))
                     first
                     :id)
        thumb-id (->> filtered-cover
                      (filter :thumbnail)
                      first
                      :id)]
    {:main main-id
     :thumbnail thumb-id}))

(defn group-by-parent
  [images]
  (let [group-key (fn [img]
                    (or (:parent_id img) (:id img)))]
    (group-by group-key images)))

(defn add-cover-image-urls
  [images pool-id model-id]
  (let [groups (group-by-parent images)]
    (vec (mapcat
          (fn [[group-id entries]]
            (let [cover-image-id (:cover_image_id (first entries))
                  default-id (:id (first entries))
                  {:keys [main thumbnail]} (if cover-image-id
                                             (filtered-cover-ids cover-image-id entries)
                                             (filtered-cover-ids default-id entries))]
              (map #(cond-> %
                      main (assoc :url
                              ;main (assoc :cover_image_url
                                  (str "/inventory/" pool-id "/models/" model-id "/images/" main))
                       ;thumbnail (assoc :cover_image_thumb
                      thumbnail (assoc :thumbnail_url
                                       (str "/inventory/" pool-id "/models/" model-id "/images/" thumbnail "/thumbnail")))
                   entries)))
          groups))))

(defn fetch-image-attributes [tx model-id pool-id]
  (let [query (-> (sql/select
                   :i.id
                   :i.filename
                   [[[:raw "CASE WHEN m.cover_image_id = i.id THEN TRUE ELSE FALSE END"]] :is_cover]
                   :i.target_id
                   :i.parent_id
                   :i.thumbnail
                   :m.cover_image_id)
                  (sql/from [:models :m])
                  (sql/right-join [:images :i] [:= :i.target_id :m.id])
                  (sql/where [:= :m.id model-id])
                  (sql/where [:= :m.type "Model"])
                  (sql/order-by [:i.filename :desc] [:i.content_type :desc])
                  sql-format)
        images (->> (jdbc/execute! tx query)
                    ;(add-cover-image-urls % pool-id model-id) ;; fix this, has to be at first position
                    (#(add-cover-image-urls % pool-id model-id))
                    (filter #(not (:thumbnail %)))
                    (map #(dissoc % :target_id :parent_id :cover_image_id)))]
    images))

(defn fetch-accessories [tx model-id]
  (let [query (-> (sql/select :a.id :a.name)
                  (sql/from [:accessories :a])
                  (sql/left-join [:accessories_inventory_pools :aip] [:= :a.id :aip.accessory_id])
                  (sql/where [:= :a.model_id model-id])
                  (sql/order-by :a.name)
                  sql-format)]
    (jdbc/execute! tx query)))

;(defn get-one-thumbnail-query [tx id]
;  (jdbc/execute-one! tx (-> (sql/select :id :target_id :thumbnail :filename)
;                          (sql/from :images)
;                          (sql/where [:and
;                                      [:= :target_id id]
;                                      [:= :thumbnail true]])
;                          sql-format)))
;
;(defn fetch-thumbnails-for-ids [tx ids]
;  (vec (map #(get-one-thumbnail-query tx %) ids)))
;
;(defn create-url [pool_id model_id type cover_image_id]
;  (str "/inventory/" pool_id "/models/" model_id "/images/" cover_image_id))
;
;(defn apply-cover-image-urls [models thumbnails pool_id]
;  (map
;    (fn [model]
;      (let [cover-image-id (:cover_image_id model)
;            origin-table (:origin_table model)
;            thumbnail-id (->> thumbnails
;                           (filter #(= (:target_id %) (:id model)))
;                           first
;                           :id)]
;        (cond
;          (and (= "models" origin-table) cover-image-id)
;          (assoc model :cover_image_url (create-url pool_id (:id model) "images" cover-image-id))
;
;          (and (= "models" origin-table) thumbnail-id)
;          (assoc model :cover_image_url (str (create-url pool_id (:id model) "images" thumbnail-id)  "/thumbnail"))
;
;          :else model)))
;    models))

(defn fetch-compatibles [tx model-id pool-id]
  (let [query (-> (sql/select :mm.id :mm.product :mm.version ["models" :origin_table] :mm.cover_image_id)
                  (sql/from [:models_compatibles :mc])
                  (sql/left-join [:models :mm] [:= :mc.compatible_id :mm.id])
                  (sql/where [:= :mc.model_id model-id])
                  sql-format)
        models (-> (jdbc/execute! tx query) remove-nil-entries-fnc)
        ids (mapv :id models)
        thumbnails (fetch-thumbnails-for-ids tx ids)
        models (apply-cover-image-urls models thumbnails pool-id)]
    (map #(dissoc % :origin_table) models)))

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

(defn get-resource [request]
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

            attachments (fetch-attachments tx model-id pool-id)
            image-attributes (fetch-image-attributes tx model-id pool-id)
            accessories (fetch-accessories tx model-id)
            compatibles (fetch-compatibles tx model-id pool-id)
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
