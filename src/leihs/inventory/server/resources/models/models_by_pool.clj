(ns leihs.inventory.server.resources.models.models-by-pool
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.pagination :refer [create-paginated-response fetch-pagination-params]]
   [next.jdbc.sql :as jdbc]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]])
  (:import (java.time LocalDateTime)
           (java.util UUID)))

(defn remove-select [query]
  (-> query
      (dissoc :select :select-distinct)))

(defn item-query [query item-id]
  (-> query
      remove-select
      (sql/select-distinct [:i.*])
      (sql/join [:items :i] [:= :m.id :i.model_id])
      (cond-> item-id
        (sql/where [:= :i.id item-id]))))

(defn entitlements-query [query entitlement-id]
  (-> query
      remove-select
      (sql/select-distinct [:e.*])
      (sql/join [:entitlements :e] [:= :m.id :e.model_id])
      (cond-> entitlement-id
        (sql/where [:= :e.id entitlement-id]))))

(defn model-links-query [query model-links-id pool_id]
  (-> query
      remove-select
      (sql/select-distinct [:ml.*])
      (cond-> (nil? pool_id) (sql/join [:model_links :ml] [:= :m.id :ml.model_id]))
      (cond-> model-links-id
        (sql/where [:= :ml.id model-links-id]))))

(defn properties-query [query properties-id]
  (-> query
      remove-select
      (sql/select-distinct [:p.*])
      (sql/join [:properties :p] [:= :m.id :p.model_id])
      (cond-> properties-id
        (sql/where [:= :p.id properties-id]))))

(defn accessories-query
  ([query accessories-id]
   (accessories-query query accessories-id "n/d"))
  ([query accessories-id type]
   (-> query
       remove-select
       (sql/select-distinct [:a.*])
       (sql/join [:accessories :a] [:= :m.id :a.model_id])
       (cond-> accessories-id
         (sql/where [:= :a.id accessories-id])))))

(defn attachments-query
  ([query attachment-id]
   (attachments-query query attachment-id "n/d"))
  ([query attachment-id type]
   (-> query
       remove-select
       (sql/select-distinct :a.id :a.content :a.filename :a.item_id)
       (sql/join [:attachments :a] [:= :m.id :a.model_id])
       (cond-> attachment-id
         (sql/where [:= :a.id attachment-id])))))

(defn base-pool-query [query pool-id type]
  (-> query
      (sql/from [:models :m])
      (cond->
       pool-id (sql/join [:model_links :ml] [:= :m.id :ml.model_id])
       pool-id (sql/join [:model_groups :mg] [:= :mg.id :ml.model_group_id])
       pool-id (sql/join [:inventory_pools_model_groups :ipmg] [:= :mg.id :ipmg.model_group_id])
       pool-id (sql/join [:inventory_pools :ip] [:= :ip.id :ipmg.inventory_pool_id])
       pool-id (sql/where [:= :ip.id [:cast pool-id :uuid]]))))

(defn extract-option-by-uri [input-str]
  (let [valid-segments ["properties" "items" "accessories" "attachments" "entitlements" "model-links"]
        last-segment (-> input-str
                         (clojure.string/split #"/")
                         last)]
    (if (some #(= last-segment %) valid-segments)
      last-segment
      nil)))

(defn valid-get-request? [request]
  (let [method (:request-method request)
        uri (:uri request)
        uuid-regex #"([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})$"]
    (and (= method :get)
         (not (re-find uuid-regex uri)))))

(defn- pagination-response [request base-query]
  (let [{:keys [page size]} (fetch-pagination-params request)
        tx (:tx request)]
    (create-paginated-response base-query tx size page)))

(defn get-models-handler
  ([request]
   (get-models-handler request false))
  ([request with-pagination?]
   (let [tx (:tx request)
         {:keys [pool_id model_id item_id properties_id accessories_id attachments_id entitlement_id model_link_id]} (path-params request)
         option-type (extract-option-by-uri (:uri request))
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
     (cond
       (and (nil? with-pagination?) (valid-get-request? request)) (pagination-response request base-query)
       with-pagination? (pagination-response request base-query)
       :else (jdbc/query tx (-> base-query sql-format))))))

(defn get-models-of-pool-with-pagination-handler [request]
  (response (get-models-handler request true)))

(defn get-models-of-pool-auto-pagination-handler [request]
  (response (get-models-handler request nil)))

(defn get-models-of-pool-handler [request]
  (let [result (get-models-handler request)]
    (response result)))

(defn to-uuid [value]
  (try
    (if (instance? String value) (UUID/fromString value) value)
    (catch Exception e
      value)))

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
      (let [res (jdbc/insert! tx :models model)
            model-id (:id res)]
        (doseq [category-id categories]
          (jdbc/insert! tx :model_links {:model_id model-id :model_group_id (to-uuid category-id)})
          (jdbc/insert! tx :inventory_pools_model_groups {:inventory_pool_id (to-uuid pool-id) :model_group_id (to-uuid category-id)}))
        (if res
          (response res)
          (bad-request {:error "Failed to create model"})))
      (catch Exception e
        (error "Failed to create model" e)
        (bad-request {:error "Failed to create model" :details (.getMessage e)})))))

(defn update-model-handler-by-pool [request]
  (let [model-id (get-in request [:path-params :id])
        body-params (:body-params request)
        tx (:tx request)]
    (try
      (let [res (jdbc/update! tx :models body-params ["id = ?::uuid" model-id])]
        (if (= 1 (:next.jdbc/update-count res))
          (response {:message "Model updated" :id model-id})
          (bad-request {:error "Failed to update model" :details "Model not found"})))
      (catch Exception e
        (error "Failed to update model" e)
        (bad-request {:error "Failed to update model" :details (.getMessage e)})))))

(defn delete-model-handler-by-pool [request]
  (let [tx (:tx request)
        model-id (get-in request [:path-params :id])]
    (try
      (let [res (jdbc/delete! tx :models ["id = ?::uuid" model-id])]
        (if (= 1 (:next.jdbc/update-count res))
          (response {:message "Model deleted" :id model-id})
          (bad-request {:error "Failed to delete model" :details "Model not found"})))
      (catch Exception e
        (error "Failed to delete model" e)
        (status (bad-request {:error "Failed to delete model" :details (.getMessage e)}) 409)))))
