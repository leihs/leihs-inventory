(ns leihs.inventory.server.resources.models.models-by-pool
  (:require
   [clojure.set]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.core :refer [presence]]
   [leihs.inventory.server.resources.models.queries :refer [accessories-query attachments-query base-pool-query
                                                            entitlements-query item-query
                                                            model-links-query properties-query
                                                            with-items without-items with-search]]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.core :refer [single-entity-get-request?]]
   [leihs.inventory.server.utils.helper :refer [convert-map-if-exist url-ends-with-uuid?]]
   [leihs.inventory.server.utils.pagination :refer [fetch-pagination-params pagination-response create-pagination-response]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [debug error]])
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

(defn apply-is_deleted-context-if-valid
  "setups base-query for is_deletable and references:
  - m: models
  - i: items
  - it: items (for items that are children of item i)"
  [is_deletable]
  (-> (sql/select-distinct :m.*
                           [[:raw "CASE
                                   WHEN m.is_package = true AND m.type = 'Model' AND i.id IS NULL AND it.id IS NULL THEN true
                                   WHEN m.is_package = false AND m.type = 'Model' AND i.id IS NULL AND it.id IS NULL THEN true
                                   WHEN m.is_package = false AND m.type = 'Software' AND i.id IS NULL AND it.id IS NULL THEN true
                                   ELSE false
                                   END"]
                            :is_deletable])
      (sql/from [:models :m])
      (sql/left-join [:items :i] [:= :m.id :i.model_id])
      (sql/left-join [:items :it] [:= :it.parent_id :i.id])))

(defn apply-is_deleted-where-context-if-valid [base-query is_deletable]
  (if (nil? is_deletable)
    base-query
    (-> (sql/select-distinct :*)
        (sql/from [[base-query] :wrapped_query])
        (sql/where [:= :wrapped_query.is_deletable is_deletable]))))

(defn get-models-handler
  ([request]
   (get-models-handler request false))
  ([request with-pagination?]
   (let [tx (:tx request)
         {:keys [pool_id]} (path-params request)
         {:keys [with_items retired search]} (query-params request)
         {:keys [page size]} (fetch-pagination-params request)
         query (-> base-pool-query
                   (cond->
                    (and pool_id (true? with_items))
                     (with-items pool_id :retired retired)
                     (and pool_id (false? with_items))
                     without-items
                     (and pool_id (presence search))
                     (with-search search)))]
     (if (url-ends-with-uuid? (:uri request))
       (let [res (jdbc/execute-one! tx (-> query sql-format))]
         (if res
           (response res)
           (status 404)))
       (response (create-pagination-response request query with-pagination?))))))

(defn get-models-of-pool-with-pagination-handler [request]
  (get-models-handler request true))

(defn get-models-of-pool-auto-pagination-handler [request]
  (get-models-handler request nil))

(defn get-models-of-pool-handler [request]
  (let [result (get-models-handler request)]
    result))

;;  ------------

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
