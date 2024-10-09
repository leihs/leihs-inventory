(ns leihs.inventory.server.resources.models.main
  (:require
   [clojure.set]
   [honey.sql :refer [format]
    :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [path-params]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]])
  (:import (java.time LocalDateTime)))

(defn get-models-handler [request]
  (let [tx (:tx request)
        id (get-in request [:path-params :id])
        models-query (-> (sql/select :*)
                       (sql/from :models)
                       (sql/order-by :models.product)

                       (cond-> id (sql/where [:= :models.id [:cast id :uuid]]))

                       (sql/limit 10))
        result (-> models-query
                 sql-format
                 (->> (jdbc/query tx)))]

    (cond
      (nil? id) {:body result}
      (nil? (first result)) {:status 204}
      :else {:body (first result)})))

(defn get-models-compatible-handler [request]
  (try
    (let [tx (:tx request)
          model_id (-> request path-params :model_id)
          query (-> (sql/select [:m.id :model_id] :m2.*)
                  (sql/from [:models_compatibles :mc])
                  (sql/join [:models :m] [:= :mc.model_id :m.id])
                  (sql/join [:models :m2] [:= :mc.compatible_id :m2.id])
                  (cond-> model_id (sql/where [:= :m.id model_id]))
                  (sql/limit 10)
                  sql-format)
          result (jdbc/query tx query)]
      (response result))
    (catch Exception e
      (error "Failed to get user" e)
      (bad-request {:error "Failed to get user" :details (.getMessage e)}))))



;def base-query (-> (sql/from :models)
;
;                 (cond-> filter-manufacturer
;                   (sql/where [:ilike :models.manufacturer (str "%" filter-manufacturer "%")]))
;
;                 (cond-> filter-product
;                   (sql/where [:ilike :models.product (str "%" filter-product "%")]))
;
;                 (sql/order-by sort-by))
;
;(defn create-base-query [select base-query]
;
;  (let [
;        base-query (->
;
;                     (cond-> (nil? select)
;                       (sql/select [[:%count.* :count]])    ;; fnc to use [[:%count.* :count]] in first query and specified select for results
;                       (sql/select select))
;
;                     base-query
;
;                     )
;
;        ])
;
;  )
;
;(create-base-query nil base-query)


;; Define base-query to include conditions and sorting
;(def base-query
;  (-> (sql/from :models)
;    (cond-> filter-manufacturer
;      (sql/where [:ilike :models.manufacturer (str "%" filter-manufacturer "%")]))
;    (cond-> filter-product
;      (sql/where [:ilike :models.product (str "%" filter-product "%")]))
;    (sql/order-by sort-by)))

;; Function to create the base query with dynamic select
(defn create-base-query
  ([base-query]
   (create-base-query nil base-query)
   )

  ([select base-query]
   (let [

         p (println ">o> base-query1" base-query)

         base-query (dissoc base-query :order-by)
         p (println ">o> base-query2" base-query)

         base-query (when (nil? select) (dissoc base-query :select))
         p (println ">o> base-query3" base-query)



         res (-> base-query

               ;(cond-> (nil? select) (sql/select [[:%count.* :count]])))
               (cond-> (nil? select) (sql/select :%count.*)))

         ;(cond->
         ;  (nil? select) (sql/select :%count.*)
         ;  (not (nil? select)) (sql/select select)))


         p (println ">o> base-query4" res)

         ] res)
   )
  )


(defn create-count-query [base-query]
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




(defn pr [str fnc]
  ;(println ">oo> HELPER / " str fnc)(println ">oo> HELPER / " str fnc)
  (println ">oo> " str fnc)
  fnc
  )

(defn get-models-handler [request]
  (let [tx (:tx request)

        p (println ">o> abc1")

        query-params (get-in request [:parameters :query])
        page (or (:page query-params) 1)
        per_page (or (:size query-params) 10)
        offset (* (dec page) per_page)

        ;; Sorting
        sort-by (case (:sort_by query-params)
                  :manufacturer-asc [:models.manufacturer :asc]
                  :manufacturer-desc [:models.manufacturer :desc]
                  :product-asc [:models.product :asc]
                  :product-desc [:models.product :desc]
                  [:models.product :asc])                   ;; default sorting
        p (println ">o> abc2")

        ;; Filtering
        filter-manufacturer (:filter_manufacturer query-params)
        filter-product (:filter_product query-params)

        base-query (->
                     (sql/select :*)                        ;; fnc to use [[:%count.* :count]] in first query and specified select for results
                     (sql/from :models)

                     (cond-> filter-manufacturer
                       (sql/where [:ilike :models.manufacturer (str "%" filter-manufacturer "%")]))

                     (cond-> filter-product
                       (sql/where [:ilike :models.product (str "%" filter-product "%")]))

                     (sql/order-by sort-by)
                     )

        p (println ">o> abc3")
        ;;; Pagination
        ;;total-products-query (-> base-query
        ;total-products-query (->
        ;
        ;                       ;(sql/select [[:raw "count(*)"]])
        ;
        ;                       (sql/select :%count.*)
        ;
        ;                       ;(sql/select-count)
        ;
        ;                       (sql/from :models)
        ;                       ;(create-base-query base-query)
        ;
        ;                       ;println
        ;
        ;                       sql-format
        ;                       ;println
        ;                       (->> (jdbc/query tx)))
        ;p (println ">o> abc4a.count" total-products-query)


        total-products-query (-> (create-count-query base-query)
                               sql-format
                               ;println
                               ;(->> (jdbc/execute-one! tx)))
                               (->> (jdbc/query tx))
                               first
                               )
        p (println ">o> abc4")


        p (println ">o> total_products1.new=" total-products-query)
        total_products (:count total-products-query)

        p (println ">o> total_products2.????=" total_products)

        p (println ">o> abc5")

        total_pages (int (Math/ceil (/ total_products (float per_page))))
        ;
        ;paginated-query (-> (create-base-query [:*] base-query)
        ;paginated-query (-> (create-base-query [:*] base-query)
        paginated-query (->  base-query
                          (sql/limit per_page)
                          (sql/offset offset)
                          sql-format
                          (->> (jdbc/query tx)))

        p (println ">o> paginated-query" paginated-query)
        paginated_products (mapv identity paginated-query)

        p (println ">o> paginated_products" paginated_products)

        pagination-info {:total_records total_products
                         :current_page page
                         :total_pages total_pages
                         :next_page (when (< page total_pages) (inc page))
                         :prev_page (when (> page 1) (dec page))}


        ;pagination-info {:total_records 0
        ;                 :current_page 0
        ;                 :total_pages 0
        ;                 :next_page 0
        ;                 :prev_page 0}
        ;paginated_products []

        ]

    {:body {:data paginated_products
            :pagination pagination-info}}))

(defn create-model-handler [request]
  (let [created_ts (LocalDateTime/now)
        body-params (:body-params request)
        tx (:tx request)
        model body-params
        model (assoc body-params
                :created_at created_ts
                :updated_at created_ts)]

    (try
      (let [res (jdbc/insert! tx :models model)]
        (if res
          (response res)
          (bad-request {:error "Failed to create model"})))

      (catch Exception e
        (error "Failed to create model" e)
        (bad-request {:error "Failed to create model" :details (.getMessage e)})))))

(defn update-model-handler [request]
  (let [model-id (get-in request [:path-params :id])
        body-params (:body-params request)
        tx (:tx request)
        model body-params]

    (try
      (let [res (jdbc/update! tx :models model ["id = ?::uuid" model-id])]

        (if (= 1 (:next.jdbc/update-count res))
          (response {:message "Model updated" :id model-id})
          (bad-request {:error "Failed to update model" :details "Model not found"})))

      (catch Exception e
        (error "Failed to update model" e)
        (bad-request {:error "Failed to update model" :details (.getMessage e)})))))

(defn delete-model-handler [request]
  (let [tx (:tx request)
        model-id (get-in request [:path-params :id])]

    (try
      (let [res (jdbc/delete! tx :models ["id = ?::uuid" model-id])]

        (if (= 1 (:next.jdbc/update-count res))
          (response {:message "Model deleted" :id model-id})
          (bad-request {:error "Failed to delete model" :details "Model not found"})))

      (catch Exception e
        (error "Failed to delete model" e)
        (status (bad-request {:error "Failed to delete model"
                              :details (.getMessage e)}) 409)))))
