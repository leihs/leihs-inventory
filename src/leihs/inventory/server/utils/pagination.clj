(ns leihs.inventory.server.utils.pagination
  (:require
   [clojure.java.io :as io]
            [clojure.string :as str]

   [honey.sql :refer [format]
    :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]]

            ;[leihs.core.auth.session :as session]
            ;[leihs.core.core :refer [presence]]
            ;[leihs.core.db :as db]
            ;[leihs.core.ring-audits :as ring-audits]
            ;[leihs.core.routing.dispatch-content-type :as dispatch-content-type]
            ;[leihs.inventory.server.routes :as routes]
            ;[leihs.inventory.server.utils.response_helper :as rh]
            ;[muuntaja.core :as m]
            ;[reitit.coercion.schema]
            ;[reitit.coercion.spec]
            ;[reitit.dev.pretty :as pretty]
            ;[reitit.ring :as ring]
            ;[reitit.ring.coercion :as coercion]
            ;[reitit.ring.middleware.exception :as exception]
            ;[reitit.ring.middleware.multipart :as multipart]
            ;[reitit.ring.middleware.muuntaja :as muuntaja]
            ;[reitit.ring.middleware.parameters :as parameters]
            ;[reitit.swagger :as swagger]
            ;[reitit.swagger-ui :as swagger-ui]
            ;[ring.middleware.cookies :refer [wrap-cookies]]
            ;[ring.util.response :as response]
   )

  ;(:import [java.net URL JarURLConnection]
  ;         [java.util.jar JarFile])
  )





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

