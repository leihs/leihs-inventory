(ns leihs.inventory.server.resources.models.models-by-pool
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.models.queries :refer [accessories-query attachments-query base-pool-query
                                                            entitlements-query item-query
                                                            model-links-query properties-query]]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.core :refer [single-entity-get-request?]]
   [leihs.inventory.server.utils.helper :refer [convert-map-if-exist]]
   [leihs.inventory.server.utils.pagination :refer [fetch-pagination-params pagination-response create-pagination-response]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]])
  (:import [java.net URL JarURLConnection]
           (java.time LocalDateTime)
           [java.util.jar JarFile]))

(defn- extract-option-type-from-uri [input-str]
  (let [valid-segments ["properties" "items" "accessories" "attachments" "entitlements" "model-links"]
        last-segment (-> input-str
                         (clojure.string/split #"/")
                         last)]
    (if (some #(= last-segment %) valid-segments)
      last-segment
      nil)))

(defn get-models-handler
  ([request]
   (get-models-handler request false))
  ([request with-pagination?]
   (let [tx (:tx request)
         {:keys [pool_id model_id item_id properties_id accessories_id attachments_id entitlement_id model_link_id]} (path-params request)
         option-type (extract-option-type-from-uri (:uri request))
         query-params (query-params request)
         {:keys [page size]} (fetch-pagination-params request)
         sort-by (case (:sort_by query-params)
                   :manufacturer-asc [:m.manufacturer :asc]
                   :manufacturer-desc [:m.manufacturer :desc]
                   :product-asc [:m.product :asc]
                   :product-desc [:m.product :desc]
                   nil)
         filter-manufacturer (if-not model_id (:filter_manufacturer query-params) nil)
         filter-product (if-not model_id (:filter_product query-params) nil)
         base-query (-> (sql/select-distinct :m.*)
                        ((fn [query] (base-pool-query query pool_id option-type)))
                        (cond-> (or item_id (= option-type "items"))
                          ((fn [q] (item-query q item_id))))
                        (cond-> (or properties_id (= option-type "properties"))
                          ((fn [q] (properties-query q properties_id))))
                        (cond-> (or accessories_id (= option-type "accessories"))
                          ((fn [q] (accessories-query q accessories_id option-type))))
                        (cond-> (or attachments_id (= option-type "attachments"))
                          ((fn [q] (attachments-query q attachments_id option-type))))
                        (cond-> (or entitlement_id (= option-type "entitlements"))
                          ((fn [q] (entitlements-query q entitlement_id))))
                        (cond-> (or model_link_id (= option-type "model-links"))
                          ((fn [q] (model-links-query q model_link_id pool_id))))
                        (cond-> filter-manufacturer
                          (sql/where [:ilike :m.manufacturer (str "%" filter-manufacturer "%")]))
                        (cond-> filter-product
                          (sql/where [:ilike :m.product (str "%" filter-product "%")]))
                        (cond-> model_id (sql/where [:= :m.id model_id]))
                        (cond-> (and sort-by model_id) (sql/order-by sort-by)))]
     (create-pagination-response request base-query with-pagination?))))

(defn get-models-of-pool-with-pagination-handler [request]
  (response (get-models-handler request true)))

(defn get-models-of-pool-auto-pagination-handler [request]
  (response (get-models-handler request nil)))

(defn get-models-of-pool-handler [request]
  (let [result (get-models-handler request)]
    (response result)))

;;  ------------

(defn create-model-handler-by-pool [request]
  (let [created_ts (LocalDateTime/now)
        model-id (get-in request [:path-params :model_id])
        pool-id (get-in request [:path-params :pool_id])
        body-params (:body-params request)
        tx (:tx request)

        p (println ">o> create-model-handler-by-pool.body-params" body-params)

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
        (error "Failed to create model" (.getMessage e))
        (error "Failed to create model" (.getMessage e))
        (bad-request {:error "Failed to create model" :details (.getMessage e)})))))

(defn update-model-handler-by-pool [request]
  (let [model-id (get-in request [:path-params :model_id])
        body-params (:body-params request)
        tx (:tx request)]
    (try
      (let [res (jdbc/execute! tx (-> (sql/update :models)
                                      (sql/set (convert-map-if-exist body-params))
                                      (sql/where [:= :id (to-uuid model-id)])
                                      (sql/returning :*)
                                      sql-format))]
        (if (= 1 (count res))
          (response res)

          (bad-request {:error "Failed to update model" :details "Model not found"})))
      (catch Exception e
        (error "Failed to update model" e)
        (bad-request {:error "Failed to update model" :details (.getMessage e)})))))

(defn delete-model-handler-by-pool [request]
  (let [tx (:tx request)
        model-id (get-in request [:path-params :model_id])]
    (try
      (let [res (jdbc/execute! tx (-> (sql/delete-from :models)
                                      (sql/where [:= :id (to-uuid model-id)])
                                      (sql/returning :*)
                                      sql-format))]
        (if (= 1 (count res))
          (response res)
          (bad-request {:error "Failed to delete model" :details "Model not found"})))
      (catch Exception e
        (error "Failed to delete model" e)
        (status (bad-request {:error "Failed to delete model" :details (.getMessage e)}) 409)))))
