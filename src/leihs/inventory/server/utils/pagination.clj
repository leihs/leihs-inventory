(ns leihs.inventory.server.utils.pagination
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [query-params]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]]
   ))

(defn- create-count-query [base-query]
  (let [

        p (println ">o> base-query1" base-query)

        base-query (dissoc base-query :order-by)
        p (println ">o> base-query2" base-query)
        base-query (dissoc base-query :select)

        ;base-query (when (nil? select) (dissoc base-query :select))
        p (println ">o> base-query3" base-query)

        res (-> base-query
              (sql/select :%count.*))
        ;(cond-> (nil? select) (sql/select :%count.*)))
        p (println ">o> base-query4" res)

        ] res)
  )


; Function to fetch total product count
(defn- fetch-total-products [base-query tx]
  (println ">o> abc3")
  (let [total-products-query (-> (create-count-query base-query)
                               sql-format
                               (->> (jdbc/query tx))
                               first)]
    (println ">o> total_products.new=" total-products-query)
    (:count total-products-query)))

; Function to fetch paginated product data
(defn- fetch-paginated-products [base-query tx per_page offset]
  (let [paginated-query (-> base-query
                          (sql/limit per_page)
                          (sql/offset offset)
                          sql-format
                          (->> (jdbc/query tx)))]
    (println ">o> paginated-query" paginated-query)
    (mapv identity paginated-query)))

; Main function to call the above functions and return the response
(defn create-paginated-response [base-query tx per_page page]
  (let [total-products (fetch-total-products base-query tx)
        total-pages (int (Math/ceil (/ total-products (float per_page))))
        offset (* (dec page) per_page)
        paginated-products (fetch-paginated-products base-query tx per_page offset)
        pagination-info {:total_records total-products
                         :current_page page
                         :per_page per_page
                         :total_pages total-pages
                         :next_page (when (< page total-pages) (inc page))
                         :prev_page (when (> page 1) (dec page))}]
    (println ">o> total_products" total-products)
    (println ">o> paginated_products" paginated-products)
    {:body {:data paginated-products
            :pagination pagination-info}}))


;; Usage: {:keys [page per-page offset]} (fetch-pagination-params request)
(defn fetch-pagination-params
  [request]
  (let [query-params (query-params request)
        ;; Retrieve `page` and `size` from query parameters, defaulting to 1 and 10 respectively
        page (Integer. (or (:page query-params) "1"))
        per-page (Integer. (or (:size query-params) "10"))
        offset (* (dec page) per-page)]
    ;; Return a map containing pagination details
    {:page page
     :per-page per-page
     :offset offset}))


