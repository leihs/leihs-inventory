(ns leihs.inventory.server.resources.pool.items.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.models.queries :refer [accessories-query attachments-query]]
   [leihs.inventory.server.resources.utils.request :refer [path-params]]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.pagination :refer [create-paginated-response fetch-pagination-params]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]]))

(defn base-pool-query [query pool-id]
  (-> query
      (sql/from [:items :i])
      (sql/join [:rooms :r] [:= :r.id :i.room_id])
      (sql/join [:models :m] [:= :m.id :i.model_id])
      (sql/join [:buildings :b] [:= :b.id :r.building_id])
      (cond->
       pool-id (sql/join [:inventory_pools :ip] [:= :ip.id :i.inventory_pool_id])
       pool-id (sql/where [:= :ip.id [:cast pool-id :uuid]]))))

(defn base-pool-query-distinct [query pool-id]
  (-> query
      (sql/from [:items :i])
      (sql/join [:models :m] [:= :m.id :i.model_id])
      (cond-> pool-id (sql/where [:= :i.inventory_pool_id [:cast pool-id :uuid]]))
      (sql/group-by :m.product :i.model_id :i.inventory_code :i.inventory_pool_id :i.retired :m.is_package :i.id :i.parent_id)))

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
         {:keys [pool_id item_id]} (path-params request)
         {:keys [page size]} (fetch-pagination-params request)
         {:keys [search_term not_packaged packages retired result_type]} (query-params request)
         ;sort-by (case (:sort_by query-params)
         ;          :manufacturer-asc [:m.manufacturer :asc]
         ;          :manufacturer-desc [:m.manufacturer :desc]
         ;          :product-asc [:m.product :asc]
         ;          :product-desc [:m.product :desc]
         ;          nil)

         ;filter-manufacturer (if-not model_id (:filter_manufacturer query-params) nil)
         ;filter-product (if-not model_id (:filter_product query-params) nil)

         ;; TODO: missing
         ;warranty_expiration (present within properties)
         ;current_location
         ;location
         ;type
         ;can_destroy
         ;children

         base-select (cond
                       (= result_type "Distinct") (sql/select-distinct-on [:m.product]
                                                                          :i.retired :i.parent_id :i.id
                                                                          :m.is_package
                                                                          :i.inventory_code
                                                                          :i.model_id
                                                                          :i.inventory_pool_id
                                                                          :m.product)
                       (= result_type "Min") (sql/select :i.retired :i.parent_id :i.id :i.inventory_code :i.model_id :m.is_package)
                       :else (sql/select :m.is_package :i.* [:b.name :building_name] [:r.name :room_name]))

         base-query (-> base-select
                        ((fn [query]
                           (if (= result_type "Distinct")
                             (base-pool-query-distinct query pool_id)
                             (base-pool-query query pool_id))))

                        (cond-> item_id (sql/where [:= :i.id item_id]))

                        (cond-> (= true retired) (sql/where [:is-not :i.retired nil]))
                        (cond-> (= false retired) (sql/where [:is :i.retired nil]))

                        (cond-> (= true packages) (sql/where [:= :m.is_package true]))
                        (cond-> (= false packages) (sql/where [:= :m.is_package false]))

                        (cond-> (= true not_packaged) (sql/where [:is :i.parent_id nil]))
                        (cond-> (= false not_packaged) (sql/where [:is-not :i.parent_id nil]))

                        (cond-> (not (empty? search_term))
                          (sql/where [:or [:ilike :i.inventory_code (str "%" search_term "%")] [:ilike :m.product (str "%" search_term "%")]
                                      [:ilike :m.manufacturer (str "%" search_term "%")]]))

                        (cond-> item_id (sql/where [:= :i.id item_id]))

                        (cond-> (and sort-by item_id) (sql/order-by item_id)))]

     (cond
       (= result_type "Distinct") (jdbc/query tx (-> base-query sql-format))
       (and (nil? with-pagination?) (valid-get-request? request)) (pagination-response request base-query)
       with-pagination? (pagination-response request base-query)
       :else (jdbc/query tx (-> base-query sql-format))))))

(defn get-items-of-pool-with-pagination-handler [request]
  (response (get-items-handler request true)))

(defn get-items-of-pool-handler [request]
  (let [result (get-items-handler request)]
    (response result)))

