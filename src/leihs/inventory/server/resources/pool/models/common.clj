(ns leihs.inventory.server.resources.pool.models.common
  (:require
   [clojure.set]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.core :refer [presence]]
   [leihs.inventory.server.resources.pool.models.queries :refer [base-inventory-query
                                                                 filter-by-type
                                                                 from-category with-items with-search
                                                                 without-items]]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.helper :refer [url-ends-with-uuid?]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response fetch-pagination-params]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [debug error]])
  (:import (java.time LocalDateTime)
           [java.util.jar JarFile]))

(defn- get-one-thumbnail-query [tx id]
  (jdbc/execute-one! tx (-> (sql/select :id :target_id :thumbnail :filename)
                            (sql/from :images)
                            (sql/where [:and
                                        [:= :target_id id]
                                        [:= :thumbnail true]])
                            sql-format)))

(defn fetch-thumbnails-for-ids [tx ids]
  (vec (map #(get-one-thumbnail-query tx %) ids)))

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
            (assoc :cover_image_url (create-url pool_id (:id model) "images" cover-image-id))

            (and (= "models" origin_table) thumbnail-id)
            (assoc :cover_image_thumb (str (create-url pool_id (:id model) "images" thumbnail-id) "/thumbnail")))))
      models)))
