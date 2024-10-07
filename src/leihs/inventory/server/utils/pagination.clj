(ns leihs.inventory.server.utils.pagination
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [query-params]]
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

(defn- fetch-paginated-products [base-query tx per_page offset]
  (let [paginated-query (-> base-query
                            (sql/limit per_page)
                            (sql/offset offset)
                            sql-format
                            (->> (jdbc/query tx)))]
    (mapv identity paginated-query)))

(defn create-paginated-response [base-query tx per_page page]
  (let [total_records (fetch-total-count base-query tx)
        total-pages (int (Math/ceil (/ total_records (float per_page))))
        offset (* (dec page) per_page)
        paginated-products (fetch-paginated-products base-query tx per_page offset)
        pagination-info {:total_records total_records
                         :current_page page
                         :per_page per_page
                         :total_pages total-pages
                         :next_page (when (< page total-pages) (inc page))
                         :prev_page (when (> page 1) (dec page))}]
    {:data paginated-products
     :pagination pagination-info}))

(defn fetch-pagination-params [request]
  (let [query-params (query-params request)
        page (Integer. (or (:page query-params) "1"))
        size (Integer. (or (:size query-params) "10"))]
    {:page page
     :size size}))
