(ns leihs.inventory.server.resources.models.main
  (:require
   [clojure.set]
   [honey.sql :refer [format]
    :rename {format sql-format}]
   [honey.sql.helpers :as sql]

   [leihs.inventory.server.resources.utils.request :refer [query-params]]

   [leihs.inventory.server.resources.utils.request :refer [path-params]]
   [leihs.inventory.server.utils.pagination :refer [create-paginated-response fetch-pagination-params]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]


   [ring.util.response :refer [bad-request response status]]

   [taoensso.timbre :refer [error]]

   )

  (:import [java.net URL JarURLConnection]

(java.time LocalDateTime)


  [java.util.jar JarFile])
  )

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


(defn pr [str fnc]
  ;(println ">oo> HELPER / " str fnc)(println ">oo> HELPER / " str fnc)
  (println ">oo> " str fnc)
  fnc
  )


(defn get-models-handler [request]
  (let [tx (:tx request)

        p (println ">o> abc1")

        ;query-params (get-in request [:parameters :query])
        query-params (query-params request)

        page (or (:page query-params) 1)
        per_page (or (:size query-params) 10)
        offset (* (dec page) per_page)


        {:keys [page per-page offset]} (fetch-pagination-params request)



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

        base-query (-> (sql/select :*)
                     (sql/from :models)

                     (cond-> filter-manufacturer
                       (sql/where [:ilike :models.manufacturer (str "%" filter-manufacturer "%")]))

                     (cond-> filter-product
                       (sql/where [:ilike :models.product (str "%" filter-product "%")]))

                     (sql/order-by sort-by)
                     )]
    (create-paginated-response base-query tx per_page page)))

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
