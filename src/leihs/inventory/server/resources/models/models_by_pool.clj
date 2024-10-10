(ns leihs.inventory.server.resources.models.models-by-pool
  (:require
   [clojure.set]
   [honey.sql :refer [format]
    :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.pagination :refer [create-paginated-response fetch-pagination-params]]

   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]])
  (:import (java.time LocalDateTime)))

(defn get-pools-handler [request]
  (let [tx (:tx request)
        pool_id (get-in request [:path-params :pool_id])
        model_id (get-in request [:path-params :model_id])
        ;; TODO: fix hierarchical model query
        models-query (->
                       ;(sql/with-recursive
                       ;  :model_group_links
                       ;  (sql/union
                       ;    ;; Anchor member
                       ;    (sql/select :mg.id :mg.name)
                       ;    (sql/from [:model_groups :mg])
                       ;    (sql/where [:in :mg.id
                       ;                (sql/select :pmg.model_group_id)
                       ;                (sql/from :inventory_pools_model_groups :pmg)
                       ;                (sql/where [:= :pmg.inventory_pool_id [:cast pool_id :uuid]])])
                       ;    ;; Recursive member
                       ;    (sql/select :child_mg.id :child_mg.name)
                       ;    (sql/from [:model_group_links :parent_mg])
                       ;    (sql/join [:model_group_links :mgl] [:= :parent_mg.id :mgl.parent_id])
                       ;    (sql/join [:model_groups :child_mg] [:= :mgl.child_id :child_mg.id])))

                      (sql/select :p.id
                         ;[:mg_h.id :model_group_id]
                                  [:m.id :model_id]
                                  [:p.name :pool_name]
                         ;[:mg_h.name :model_group_name]
                                  [:m.product])

                      (sql/from [:inventory_pools :p])
                      (sql/join [:inventory_pools_model_groups :pmg] [:= :p.id :pmg.inventory_pool_id])
                      (sql/join [:model_links :ml] [:= :pmg.model_group_id :ml.model_group_id])
                      (sql/join [:models :m] [:= :ml.model_id :m.id])
                      (sql/where [:= :p.id [:cast pool_id :uuid]])

                      (cond-> model_id (sql/where [:= :m.id [:cast model_id :uuid]])))

        res (-> models-query
                sql-format
                (->> (jdbc/query tx)))

        p (println ">o> res" res)]

    (response res)))

(defn get-models-handler

  ([request]
   (println ">o> get-models-handler[r]")
   (get-models-handler request false))

  ([request with-pagination?]
   ;[request ]
   (println ">o> get-models-handler[r b]")
   (let [tx (:tx request)

         p (println ">o> abc1")

;model_id (-> request path-params :id)
         ;query-params (query-params request)

         {:keys [pool_id model_id]} (path-params request)
         p (println ">o> params" pool_id model_id)

;query-params (get-in request [:parameters :query])
         query-params (query-params request)

         ;page (or (:page query-params) 1)
         ;per_page (or (:size query-params) 10)
         ;offset (* (dec page) per_page)

;{:keys [page size]} (if with-pagination? (fetch-pagination-params request) {:page nil :size nil})
         {:keys [page size]} (fetch-pagination-params request)
         p (println ">o> pagei" page size)

;; Sorting
         sort-by (case (:sort_by query-params)
                   :manufacturer-asc [:m.manufacturer :asc]
                   :manufacturer-desc [:m.manufacturer :desc]
                   :product-asc [:m.product :asc]
                   :product-desc [:m.product :desc]
                   [:m.product :asc]) ;; default sorting
         p (println ">o> abc2")

         ;; Filtering
         filter-manufacturer (if-not model_id (:filter_manufacturer query-params) nil)
         filter-product (if-not model_id (:filter_product query-params) nil)

         p (println ">o> abc3")
         base-query (-> (sql/select-distinct :m.*)
                      ;(sql/from [:models :m])

                        (sql/from [:inventory_pools :p])
                        (sql/join [:inventory_pools_model_groups :pmg] [:= :p.id :pmg.inventory_pool_id])
                        (sql/join [:model_links :ml] [:= :pmg.model_group_id :ml.model_group_id])
                        (sql/join [:models :m] [:= :ml.model_id :m.id])
                        (sql/where [:= :p.id [:cast pool_id :uuid]])

;; filter by pool
                        (cond-> filter-manufacturer
                          (sql/where [:ilike :m.manufacturer (str "%" filter-manufacturer "%")]))
                        (cond-> filter-product
                          (sql/where [:ilike :m.product (str "%" filter-product "%")]))

                        (cond-> model_id (sql/where [:= :m.id model_id]))
                        (cond-> model_id (sql/order-by sort-by)))

         p (println ">o> abc4.a" base-query)
         p (println ">o> abc4.b" (-> base-query sql-format))]
     ;(create-paginated-response base-query tx size page)

;(if model_id
     ;  ;(response (jdbc/query tx (-> base-query sql-format)))
     ;  (jdbc/query tx (-> base-query sql-format))
     ;  (let [{:keys [page size]} (fetch-pagination-params request)
     ;        ] (create-paginated-response base-query tx size page))
     ;  )

     (cond

       ;model_id (response (jdbc/query tx (-> base-query sql-format)))
       ;
       ;(and (not model_id) (not with-pagination?))
       ;(response (jdbc/query tx (-> base-query sql-format)))

       (and (not model_id) with-pagination?)
       (let [{:keys [page size]} (fetch-pagination-params request)] (create-paginated-response base-query tx size page))

       :else (jdbc/query tx (-> base-query sql-format))

       ;(and model_id with-pagination?)
       ;(let [{:keys [page size]} (fetch-pagination-params request)
       ;      ] (create-paginated-response base-query tx size page))
       ))))
(defn get-models-of-pool-with-pagination-handler [request]
  (get-models-handler request true))

(defn get-models-of-pool-handler [request]
  (println ">o> get-models-of-pool-handler")
  (let [result (get-models-handler request)] ;;FIXME
    ;{:body result}
    (response result)))

(defn create-model-handler-by-pool [request]
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

(defn update-model-handler-by-pool [request]
  (let [model-id (get-in request [:path-params :id])
        body-params (:body-params request)
        tx (:tx request)
        model body-params

        available-models (get-models-handler request) ;; FIXME

        model-id (:model_id (first available-models))]

    (try
      (let [res (jdbc/update! tx :models model ["id = ?::uuid" model-id])]

        (if (= 1 (:next.jdbc/update-count res))
          (response {:message "Model updated" :id model-id})
          (bad-request {:error "Failed to update model" :details "Model not found"})))

      (catch Exception e
        (error "Failed to update model" e)
        (bad-request {:error "Failed to update model" :details (.getMessage e)})))))

(defn delete-model-handler-by-pool [request]
  (let [tx (:tx request)
        model-id (get-in request [:path-params :id])
        available-models (get-models-handler request) ;; FIXME
        model-id (:model_id (first available-models))]

    (try
      (let [res (jdbc/delete! tx :models ["id = ?::uuid" model-id])]

        (if (= 1 (:next.jdbc/update-count res))
          (response {:message "Model deleted" :id model-id})
          (bad-request {:error "Failed to delete model" :details "Model not found"})))

      (catch Exception e
        (error "Failed to delete model" e)
        (status
         (bad-request
          {:error "Failed to delete model"
           :details (.getMessage e)})
         409)))))
