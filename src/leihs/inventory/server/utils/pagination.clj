(ns leihs.inventory.server.utils.pagination
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [query-params]]
   [leihs.inventory.server.utils.core :refer [single-entity-get-request?]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]]))

(defn- create-count-query [base-query]
  (-> base-query
      (dissoc :order-by :select :select-distinct)
      (sql/select-distinct :%count.*)))

(defn- fetch-total-count [base-query tx]
  (let [total-products-query (-> (create-count-query base-query)
                                 sql-format
                                 (->> (jdbc/query tx))
                                 first)]
    (:count total-products-query)))

(defn- fetch-paginated-rows [base-query tx per_page offset]
  (let [paginated-query (-> base-query
                            (sql/limit per_page)
                            (sql/offset offset)
                            sql-format
                            (->> (jdbc/query tx)))]
    (mapv identity paginated-query)))

(defn create-paginated-response
  ([base-query tx size page]
   (create-paginated-response base-query tx size page nil))

  ([base-query tx size page post-data-fnc]
   (let [total_records (fetch-total-count base-query tx)
         total-pages (int (Math/ceil (/ total_records (float size))))
         offset (* (dec page) size)
         paginated-products (fetch-paginated-rows base-query tx size offset)
         pagination-info {:total_records total_records
                          :current_page page
                          :per_page size
                          :total_pages total-pages
                          :next_page (when (< page total-pages) (inc page))
                          :prev_page (when (> page 1) (dec page))}

         paginated-products (if (nil? post-data-fnc) paginated-products
                                (post-data-fnc paginated-products))]
     {:data paginated-products
      :pagination pagination-info})))

(defn fetch-pagination-params [request]
  (let [query-params (query-params request)
        page (Integer. (or (:page query-params) "1"))
        size (Integer. (or (:size query-params) "10"))]
    {:page page
     :size size}))

(defn pagination-response
  ([request base-query]
   (pagination-response request base-query nil))

  ([request base-query post-data-fnc]
   (let [{:keys [page size]} (fetch-pagination-params request)
         tx (:tx request)]
     (create-paginated-response base-query tx size page post-data-fnc))))

(defn create-pagination-response [request base-query with-pagination?]
  (cond
    (and (nil? with-pagination?) (single-entity-get-request? request)) (pagination-response request base-query)
    with-pagination? (pagination-response request base-query)
    :else (jdbc/query (:tx request) (-> base-query sql-format))))