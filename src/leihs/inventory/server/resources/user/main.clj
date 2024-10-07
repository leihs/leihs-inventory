(ns leihs.inventory.server.resources.user.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [path-params]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   ;[honey.sql :refer [format format-expr] :rename {format sql-format}]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]]))

(defn get-pools-of-user-handler [request]
  (try (let [tx (:tx request)

             ;user_id (get-in request [:path-params :user_id])


             user_id (-> request path-params :user_id)
             p (println ">o> user_id" user_id (type user_id))



             ;query-params (get-in request [:parameters :query])
             ;p (println ">o> query-params1" query-params)
             ;
             ;query-params (get-in request [:parameters :query-params :user_id])
             ;p (println ">o> query-params2" query-params (type query-params))
             ;
             ;
             ;
             ;p (println ">o> abc ??1?" (-> request :params))
             ;p (println ">o> abc ??2?" (-> request :path-params :user_id) (type (-> request :path-params :user_id)))
             ;
             ;;p (println ">o> abc ??1?" (-> request :params))
             ;p (println ">o> abc ??3a?" (-> request :parameters :query :user_id) (type (-> request :parameters :query :user_id)))
             ;p (println ">o> abc ??3b?" (-> request :parameters :query ) (type (-> request :parameters :query )))
             ;
             ;
             ;
             ;p (println ">o> abc ??4?" (-> request :parameters :path :user_id) (type (-> request :parameters :path :user_id)))
             ;p (println ">o> abc ??4?" (-> request :parameters :path ) (type (-> request :parameters :path )))
             ;
             ;u-id (-> request :parameters :query :user_id)
             ;p (println ">o> query-params2.u-id" u-id (type u-id))
             ;
             ;;query-params (get-in request [:parameters :query-params]
             ;
             ;
             ;
             ;p (println ">o> query-params2.keys" (keys request))
             ;
             ;
             ;
             ;p (println ">o> user_id3" user_id (type user_id))


             ;query (-> (sql/select [:p.id :p.name :p.description])
             ;        (sql/from [:direct_access_rights :d])
             ;        (sql/join [:users :u] [:= :u.id :d.user_id])
             ;        (sql/join [:inventory_pools :p] [:= :d.inventory_pool_id :p.id])
             ;        (sql/where [:= :u.user_id user_id])
             ;        sql-format
             ;        )

             ;query (-> (sql/select [:p.id :p.name :p.description])
             ;         (sql/from [:direct_access_rights :d])
             ;         (sql/join [:users :u] [:= :u.id :d.user_id])
             ;         (sql/join [:inventory_pools :p] [:= :d.inventory_pool_id :p.id])
             ;         (sql/where [:= :u.id user_id]) ; Adjusted column name here for clarity
             ;         sql-format)


             ;query (-> (sql/select [:p.id :p.name :p.description])
             query (-> (sql/select :p.id :p.name :p.description)
                     (sql/from [:direct_access_rights :d])
                     (sql/join [:users :u] [:= :u.id :d.user_id])
                     (sql/join [:inventory_pools :p] [:= :d.inventory_pool_id :p.id])
                     (sql/where [:= :u.id user_id])         ; Adjusted column name here for clarity
                     sql-format)


             ;(defn execute-query [db]
             ;  (jdbc/query db (sql/format query)))
             ;

             ;result (-> query
             ;         sql-format
             ;         (->> (jdbc/query tx)))

             p (println ">o> query" query)
             result (jdbc/query tx query)

             p (println ">o> result" result)
             ]

         ;(cond
         ;  (nil? id) {:body result}
         ;  (nil? (first result)) {:status 204}
         ;  :else {:body (first result)})


         ;result
         (response result)
         )
       (catch Exception e
         (error "Failed to get pools of user" e)
         (bad-request {:error "Failed to get pools of user" :details (.getMessage e)}))

       )

  )

;
;
;(defn get-models-handler [request]
;  (let [tx (:tx request)
;        query-params (get-in request [:parameters :query])
;        page (or (:page query-params) 1)
;        per_page (or (:size query-params) 10)
;        offset (* (dec page) per_page)
;
;        sort-by (case (:sort_by query-params)
;                  :manufacturer-asc [:models.manufacturer :asc]
;                  :manufacturer-desc [:models.manufacturer :desc]
;                  :product-asc [:models.product :asc]
;                  :product-desc [:models.product :desc]
;                  [:models.product :asc]) ;; default sorting
;
;        filter-manufacturer (:filter_manufacturer query-params)
;        filter-product (:filter_product query-params)
;
;        base-query (-> (sql/select :*)
;                       (sql/from :models)
;
;                       (cond-> filter-manufacturer
;                         (sql/where [:ilike :models.manufacturer (str "%" filter-manufacturer "%")]))
;
;                       (cond-> filter-product
;                         (sql/where [:ilike :models.product (str "%" filter-product "%")]))
;
;                       (sql/order-by sort-by))
;
;        total-products-query (-> base-query
;                                 sql-format
;                                 (->> (jdbc/query tx)))
;
;        total_products (count total-products-query)
;
;        total_pages (int (Math/ceil (/ total_products (float per_page))))
;
;        paginated-query (-> base-query
;                            (sql/limit per_page)
;                            (sql/offset offset)
;                            sql-format
;                            (->> (jdbc/query tx)))
;
;        paginated_products (mapv identity paginated-query)
;
;        pagination-info {:total_records total_products
;                         :current_page page
;                         :total_pages total_pages
;                         :next_page (when (< page total_pages) (inc page))
;                         :prev_page (when (> page 1) (dec page))}]
;
;    {:body {:data paginated_products
;            :pagination pagination-info}}))
;
;(defn create-model-handler [request]
;  (let [created_ts (LocalDateTime/now)
;        body-params (:body-params request)
;        tx (:tx request)
;        model body-params
;        model (assoc body-params
;                     :created_at created_ts
;                     :updated_at created_ts)]
;
;    (try
;      (let [res (jdbc/insert! tx :models model)]
;        (if res
;          (response res)
;          (bad-request {:error "Failed to create model"})))
;
;      (catch Exception e
;        (error "Failed to create model" e)
;        (bad-request {:error "Failed to create model" :details (.getMessage e)})))))
;
;(defn update-model-handler [request]
;  (let [model-id (get-in request [:path-params :id])
;        body-params (:body-params request)
;        tx (:tx request)
;        model body-params]
;
;    (try
;      (let [res (jdbc/update! tx :models model ["id = ?::uuid" model-id])]
;
;        (if (= 1 (:next.jdbc/update-count res))
;          (response {:message "Model updated" :id model-id})
;          (bad-request {:error "Failed to update model" :details "Model not found"})))
;
;      (catch Exception e
;        (error "Failed to update model" e)
;        (bad-request {:error "Failed to update model" :details (.getMessage e)})))))
;
;(defn delete-model-handler [request]
;  (let [tx (:tx request)
;        model-id (get-in request [:path-params :id])]
;
;    (try
;      (let [res (jdbc/delete! tx :models ["id = ?::uuid" model-id])]
;
;        (if (= 1 (:next.jdbc/update-count res))
;          (response {:message "Model deleted" :id model-id})
;          (bad-request {:error "Failed to delete model" :details "Model not found"})))
;
;      (catch Exception e
;        (error "Failed to delete model" e)
;        (status (bad-request {:error "Failed to delete model"
;                              :details (.getMessage e)}) 409)))))
