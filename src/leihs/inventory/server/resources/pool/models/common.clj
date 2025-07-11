(ns leihs.inventory.server.resources.pool.models.common
  (:require
   [clojure.set]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [debug error]]))

(defn remove-nil-values
  "Removes all nil values from a map or a vector of maps."
  [x]
  (cond
    (map? x)   (into {} (remove (comp nil? val) x))
    (sequential? x) (mapv remove-nil-values x)
    :else x))


 (defn- get-one-thumbnail-query [tx {:keys [id cover_image_id] :as model-cover-id}]
  (let [        res (jdbc/execute-one! tx (-> (sql/select :id :target_id :thumbnail :filename)
                                      (sql/from :images)
                                      (cond-> (nil? cover_image_id) (sql/where [:= :target_id id]))
                                      (cond-> (not (nil? cover_image_id)) (sql/where [:or
                                                                                      [:= :id cover_image_id]
                                                                                      [:= :parent_id cover_image_id]]))
                                      (sql/order-by [:thumbnail :asc])
                                      sql-format))

        data (if res (assoc model-cover-id
                       :image_id (:id res))
                     model-cover-id
                     )

        ]
    data))

(defn fetch-thumbnails-for-ids [tx model-cover-ids]
  (vec (map #(get-one-thumbnail-query tx %) model-cover-ids)))

(defn create-url [pool_id model_id type cover_image_id]
  (str "/inventory/" pool_id "/models/" model_id "/images/" cover_image_id))

(defn apply-cover-image-urls [models thumbnails pool_id]
  (vec
    (map-indexed
      (fn [idx model]
        (let [cover-image-id (:cover_image_id model)
              origin_table (:origin_table model)
              thumbnail-id (-> (filter #(= (:target_id %) (:id model)) thumbnails)
                             first
                             :id)]
          (cond-> model
            (and (= "models" origin_table) cover-image-id)
            (assoc :url (create-url pool_id (:id model) "images" cover-image-id))            )))
      models)))
