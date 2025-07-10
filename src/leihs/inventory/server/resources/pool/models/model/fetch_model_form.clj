(ns leihs.inventory.server.resources.pool.models.model.fetch-model-form
  (:require
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [honey.sql :as sq :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.common :refer [remove-nil-entries-fnc]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]

   [leihs.inventory.server.resources.pool.models.common :refer [apply-cover-image-urls  fetch-thumbnails-for-ids
                                                                remove-nil-values]]

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
        image-id (->> filtered-cover
                     ;(filter #(not (:thumbnail %)))
                     first
                     :id)

     ]
        image-id
    ))

    ;    thumb-id (->> filtered-cover
    ;                  (filter :thumbnail)
    ;                  first
    ;                  :id)
    ;{:main main-id
    ; :thumbnail thumb-id}))

(defn group-by-parent
  [images]
  (let [group-key (fn [img]
                    (or (:parent_id img) (:id img)))]
    (group-by group-key images)))

;(defn apply-cover-image-or-default-url
;  [images pool-id model-id]
;
;  ;(println ">o> abc.images" images)
;
;  (let [
;        ;groups (group-by-parent images)]
;    (vec (mapcat
;          (fn [image]
;            (let [cover-image-id (:cover_image_id (first entries))
;                  default-id (:id (first entries))
;                  ;{:keys [main thumbnail]} (if cover-image-id
;                  image-id (if cover-image-id
;                                             (filtered-cover-ids cover-image-id entries)
;                                             (filtered-cover-ids default-id entries))
;
;              p (println ">o> abc.image" image)
;
;              ;p (println ">o> abc.group-id" group-id)
;              ;p (println ">o> abc.entries" entries)
;              ;p (println ">o> abc.cover-image-id" cover-image-id)
;              ;p (println ">o> abc.default-id" default-id)
;
;                  ]
;              ;(map #(cond-> %
;              ;        image-id (assoc :url
;              ;                ;main (assoc :cover_image_url
;              ;                    (str "/inventory/" pool-id "/models/" model-id "/images/" image-id))
;              ;         ;thumbnail (assoc :cover_image_thumb
;              ;        ;thumbnail (assoc :thumbnail_url
;              ;        ;                 (str "/inventory/" pool-id "/models/" model-id "/images/" thumbnail "/thumbnail"))
;              ;        )
;              ;     entries)
;
;
;              ;(map #(cond-> %
;              ;        (:image-id %) (assoc :url (str "/inventory/" pool-id "/models/" model-id "/images/" (:image-id %))))
;              ;  entries)
;
;              ))
;           images))
;    )
;  )



(defn apply-cover-image-or-default-url
  [images pool-id model-id]
  (vec (mapcat
         (fn [image]

            (println ">o> abc.image" image)


           (let [cover-image-id (:cover_image_id (first images))
                 default-id (:id (first images))
                 image-id (if cover-image-id
                            (filtered-cover-ids cover-image-id images)
                            (filtered-cover-ids default-id images))]
             ; Process the image and associate URLs
             (map #(assoc % :url (str "/inventory/" pool-id "/models/" model-id "/images/" image-id))
                  images)))
         images)))

;(defn fetch-image-attributes [tx model-id pool-id]
;  (let [query (-> (sql/select
;                   :i.id
;                   :i.filename
;                   [[[:raw "CASE WHEN m.cover_image_id = i.id THEN TRUE ELSE FALSE END"]] :is_cover]
;                   :i.target_id
;                   :i.parent_id
;                   :i.thumbnail
;                   :m.cover_image_id)
;                  (sql/from [:models :m])
;                  (sql/right-join [:images :i] [:= :i.target_id :m.id])
;                  ;(sql/where [:= :m.id model-id])
;                (sql/where [:and [:= :m.id model-id] [:= :i.thumbnail false]])
;                (sql/where [:= :m.type "Model"])
;                  (sql/order-by [:i.filename :desc] [:i.content_type :desc])
;                  sql-format)
;        images (->> (jdbc/execute! tx query)
;                    ;(add-cover-image-or-default-url % pool-id model-id) ;; fix this, has to be at first position
;                    (#(apply-cover-image-or-default-url % pool-id model-id))
;                    ;(filter #(not (:thumbnail %)))
;
;                    ;(map #(dissoc % :target_id :parent_id :cover_image_id))
;                 )]
;    images))

(defn fetch-image-attributes [tx model-id pool-id]
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
    (map (fn [{:keys [id] :as row}]
           (assoc row
             :url (str "/inventory/" pool-id "/models/" model-id "/images/" id)
             :thumbnail_url (str "/inventory/" pool-id "/models/" model-id "/images/" id "/thumbnail")))
      images)))


(defn fetch-accessories [tx model-id]
  (let [query (-> (sql/select :a.id :a.name)
                  (sql/from [:accessories :a])
                  (sql/left-join [:accessories_inventory_pools :aip] [:= :a.id :aip.accessory_id])
                  (sql/where [:= :a.model_id model-id])
                  (sql/order-by :a.name)
                  sql-format)]
    (jdbc/execute! tx query)))

(defn fetch-compatibles [tx model-id pool-id]
  (let [query (-> (sql/select :mm.id :mm.product :mm.version ["models" :origin_table] :mm.cover_image_id)
                  (sql/from [:models_compatibles :mc])
                  (sql/left-join [:models :mm] [:= :mc.compatible_id :mm.id])
                  (sql/where [:= :mc.model_id model-id])
                  sql-format)
        models (jdbc/execute! tx query)

        p (println ">o> abc.models1" models)

        models (->> models
          (fetch-thumbnails-for-ids tx)
           (map (fn [m]
                 (if-let [image-id (:image_id m)]
                   (assoc m :image_url (str "/inventory/" pool-id "/models/" (:id m) "/images/" image-id))
                   m)))
                 vec
          remove-nil-values)
        p (println ">o> abc.models2" models)

        models (remove-nil-values models)

        p (println ">o> abc.models3" models)
        ;model-cover-ids (->> models (keep :id :cover_image_id) vec)
        ;ids (mapv :id models)
        ;thumbnails (fetch-thumbnails-for-ids tx ids)
        ;models (apply-cover-image-urls models thumbnails pool-id)

    ;models (map #(dissoc % [:origin_table :cover_image_id]) models)
    ;    p (println ">o> abc.models4" models)
        ]
    models
    ))

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
                     (-> (assoc model-result
                            :attachments attachments
                            :accessories accessories
                            :compatibles compatibles
                            :properties properties
                            :images image-attributes
                            :entitlements entitlements
                            :categories categories)
                       remove-nil-values)
                     nil)]
        (if result
          (response result)
          (status
           (response {:status "failure" :message "No entry found"}) 404)))
      (catch Exception e
        (error "Failed to fetch model" e)
        (bad-request {:error "Failed to fetch model" :details (.getMessage e)})))))
