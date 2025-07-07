(ns leihs.inventory.server.resources.pool.models.main
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

(defn get-one-thumbnail-query [tx id]
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
            thumbnail (first (vec (filter #(= (:target_id %) (:id model)) thumbnails)))
            thumbnail-id (:id thumbnail)]

        (cond
          (and (= "models" origin_table) cover-image-id)
          (assoc model :cover_image_url (create-url pool_id (:id model) "images" cover-image-id))

          (and (= "models" origin_table) thumbnail-id)
          (assoc model :cover_image_url (create-url pool_id (:id model) "images" thumbnail-id))

          :else
          model)))
    models)))

(defn get-models-handler
  ([request]
   (get-models-handler request false))
  ([request with-pagination?]
   (let [tx (:tx request)
         {:keys [pool_id]} (path-params request)
         {:keys [with_items type
                 retired borrowable incomplete broken
                 inventory_pool_id owned in_stock
                 category_id
                 search before_last_check]} (query-params request)
         {:keys [page size]} (fetch-pagination-params request)
         query (-> (base-inventory-query pool_id)
                   (cond-> type (filter-by-type type))
                   (cond->
                    (and pool_id (true? with_items))
                     (with-items pool_id
                       :retired retired
                       :borrowable borrowable
                       :incomplete incomplete
                       :broken broken
                       :inventory_pool_id inventory_pool_id
                       :owned owned
                       :in_stock in_stock
                       :before_last_check before_last_check)

                     (and pool_id (false? with_items))
                     (without-items pool_id)

                     (and pool_id (presence search))
                     (with-search search))
                   (cond-> category_id
                     (#(from-category tx % category_id))))

         post-fnc (fn [models]
                    (let [ids (->> models (keep :id) vec)
                          thumbnails (->> (fetch-thumbnails-for-ids tx ids) (keep identity) vec)
                          models (apply-cover-image-urls models thumbnails pool_id)]
                      models))]
     (debug (sql-format query :inline true))

     (if (url-ends-with-uuid? (:uri request))
       (let [res (jdbc/execute-one! tx (-> query sql-format))]
         (if res
           (response res)
           (status 404)))
       (response (create-pagination-response request query with-pagination? post-fnc))))))

(defn get-models-of-pool-with-pagination-handler [request]
  (get-models-handler request true))

;###################################################################################

(defn create-model-handler-by-pool [request]
  (let [created_ts (LocalDateTime/now)
        model-id (get-in request [:path-params :model_id])
        pool-id (get-in request [:path-params :pool_id])
        body-params (:body-params request)
        tx (:tx request)
        model (assoc body-params :created_at created_ts :updated_at created_ts)
        categories (:category_ids model)
        model (dissoc model :category_ids)]
    (try
      (let [res (jdbc/execute-one! tx (-> (sql/insert-into :models)
                                          (sql/values [model])
                                          (sql/returning :*)
                                          sql-format))
            model-id (:id res)]
        (doseq [category-id categories]
          (jdbc/execute! tx (-> (sql/insert-into :model_links)
                                (sql/values [{:model_id model-id :model_group_id (to-uuid category-id)}])
                                sql-format))
          (jdbc/execute! tx (-> (sql/insert-into :inventory_pools_model_groups)
                                (sql/values [{:inventory_pool_id (to-uuid pool-id) :model_group_id (to-uuid category-id)}])
                                sql-format)))
        (if res
          (response [res])
          (bad-request {:error "Failed to create model"})))
      (catch Exception e
        (error "Failed to create model" e)
        (bad-request {:error "Failed to create model" :details (.getMessage e)})))))
