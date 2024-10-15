(ns leihs.inventory.server.resources.items.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [path-params]]

   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.pagination :refer [create-paginated-response fetch-pagination-params]]

   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]]))







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

(defn base-pool-query [query pool-id ]
  (-> query
    (sql/from [:items :i])
    (cond->
      ;pool-id (sql/join [:model_links :ml] [:= :m.id :ml.model_id])
      ;pool-id (sql/join [:model_groups :mg] [:= :mg.id :ml.model_group_id])
      ;pool-id (sql/join [:inventory_pools_model_groups :ipmg] [:= :mg.id :ipmg.model_group_id])
      pool-id (sql/join [:inventory_pools :ip] [:= :ip.id :i.inventory_pool_id])
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

(defn get-items-handler
  ([request]
   (get-items-handler request false))
  ([request with-pagination?]
   (let [tx (:tx request)
         {:keys [pool_id model_id item_id properties_id accessories_id attachments_id entitlement_id model_link_id]} (path-params request)
         ;option-type (extract-option-by-uri (:uri request))
         query-params (query-params request)
         {:keys [page size]} (fetch-pagination-params request)
         ;sort-by (case (:sort_by query-params)
         ;          :manufacturer-asc [:m.manufacturer :asc]
         ;          :manufacturer-desc [:m.manufacturer :desc]
         ;          :product-asc [:m.product :asc]
         ;          :product-desc [:m.product :desc]
         ;          nil)

         ;filter-manufacturer (if-not model_id (:filter_manufacturer query-params) nil)
         ;filter-product (if-not model_id (:filter_product query-params) nil)
         ;
         ;base-query (-> (sql/select-distinct :i.*)
         base-query (-> (sql/select :i.*)
                      ((fn [query] (base-pool-query query pool_id )))

                      ;(cond-> (or item_id (= option-type "items"))
                      ;  ((fn [q] (item-query q item_id))))


                      ;(cond-> (or properties_id (= option-type "properties"))
                      ;  ((fn [q] (properties-query q properties_id))))
                      ;(cond-> (or accessories_id (= option-type "accessories"))
                      ;  ((fn [q] (accessories-query q accessories_id option-type))))
                      ;(cond-> (or attachments_id (= option-type "attachments"))
                      ;  ((fn [q] (attachments-query q attachments_id option-type))))
                      ;(cond-> (or entitlement_id (= option-type "entitlements"))
                      ;  ((fn [q] (entitlements-query q entitlement_id))))
                      ;(cond-> (or model_link_id (= option-type "model-links"))
                      ;  ((fn [q] (model-links-query q model_link_id pool_id))))

                      ;(cond-> filter-manufacturer
                      ;  (sql/where [:ilike :m.manufacturer (str "%" filter-manufacturer "%")]))
                      ;(cond-> filter-product
                      ;  (sql/where [:ilike :m.product (str "%" filter-product "%")]))

                      (cond-> item_id (sql/where [:= :i.id item_id]))
                      (cond-> (and sort-by item_id) (sql/order-by item_id)))]
     (cond
       (and (nil? with-pagination?) (valid-get-request? request)) (pagination-response request base-query)
       with-pagination? (pagination-response request base-query)
       :else (jdbc/query tx (-> base-query sql-format))))))

(defn get-items-of-pool-with-pagination-handler [request]
  (response (get-items-handler request true)))

(defn get-items-of-pool-auto-pagination-handler [request]
  (response (get-items-handler request nil)))

(defn get-items-of-pool-handler [request]
  (let [result (get-items-handler request)]
    (response result)))













;(defn get-items-of-pool-handler [request]
;  (try
;    (let [tx (:tx request)
;          pool_id (-> request path-params :pool_id)
;          item_id (-> request path-params :id)
;          query (-> (sql/select :i.*)
;                    (sql/from [:items :i])
;                    (sql/where [:= :i.inventory_pool_id pool_id])
;                    (cond-> item_id (sql/where [:= :i.id item_id]))
;                    (sql/limit 10)
;                    sql-format)
;          result (jdbc/query tx query)]
;      (response result))
;    (catch Exception e
;      (error "Failed to get items" e)
;      (bad-request {:error "Failed to get items" :details (.getMessage e)}))))
