(ns leihs.inventory.server.utils.pagination
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.utils.core :refer [single-entity-get-request?]]
   [leihs.inventory.server.utils.request-utils :refer [query-params]]
   [next.jdbc :as jdbc]
   [ring.middleware.accept]))

(defn- fetch-total-count [base-query tx]
  (-> (sql/select [[:raw "COUNT(*)"] :total_count])
    (sql/from [[base-query] :subquery])
    sql-format
    (->> (jdbc/execute-one! tx))
    :total_count))

(defn- fetch-paginated-rows [base-query tx per_page offset]
  (let [paginated-query (-> base-query
                          (sql/limit per_page)
                          (sql/offset offset)
                          sql-format
                          (->> (jdbc/execute! tx)))]
    (mapv identity paginated-query)))

(defn set-default-pagination [size page]
  (let [size (if (and (int? size) (pos? size)) size 25)
        page (if (and (int? page) (pos? page)) page 1)]
    {:size size
     :page page}))

(defn create-paginated-response
  ([base-query tx size page]
   (create-paginated-response base-query tx size page nil))

  ([base-query tx size page post-data-fnc]
   (let [total-rows (fetch-total-count base-query tx)
         {:keys [size page]} (set-default-pagination size page)
         total-pages (int (Math/ceil (/ total-rows (float size))))
         offset (* (dec page) size)
         paginated-products (fetch-paginated-rows base-query tx size offset)
         pagination-info {:total_rows total-rows
                          :total_pages total-pages
                          :page page
                          :size size}

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

(defn fetch-pagination-params-raw [request]
  (let [query-params (query-params request)
        page (:page query-params)
        size (:size query-params)]
    {:page page
     :size size}))

(defn pagination-response
  ([request base-query]
   (pagination-response request base-query nil))

  ([request base-query post-data-fnc]
   (let [{:keys [page size]} (fetch-pagination-params request)
         tx (:tx request)]
     (create-paginated-response base-query tx size page post-data-fnc))))

(defn create-pagination-response
  "To receive a paginated response, the request must contain the query parameters `page` and `size`."

  ([request base-query with-pagination?]
   (create-pagination-response request base-query with-pagination? nil))

  ([request base-query with-pagination? post-fnc]
   (let [{:keys [page size]} (fetch-pagination-params-raw request)
         tx (:tx request)]
     (cond
       (and (or (nil? with-pagination?) (= with-pagination? false))
         (single-entity-get-request? request))

       (if post-fnc
         (-> (jdbc/execute! tx (-> base-query sql-format))
           post-fnc
           first)
         (jdbc/execute-one! tx (-> base-query sql-format)))

       ;(-> (jdbc/execute! tx (-> base-query sql-format))
       ;      cond-> post-fnc
       ;
       ;  )

       (and (or (nil? with-pagination?) with-pagination?)
         (or (some? page) (some? size)))
       (pagination-response request base-query post-fnc)

       with-pagination? (pagination-response request base-query post-fnc)

       ;post-fnc (-> (jdbc/execute! tx (-> base-query sql-format))
       ;           post-fnc)
       ;
       ;:else (jdbc/execute! tx (-> base-query sql-format))


       :else        (if post-fnc
                      (-> (jdbc/execute! tx (-> base-query sql-format))
                        post-fnc)
                      (jdbc/execute! tx (-> base-query sql-format)))


       ))))
