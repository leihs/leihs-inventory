(ns leihs.inventory.server.resources.models.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [next.jdbc.sql :as jdbc]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]])

  (:import (java.time LocalDateTime)))

(defn get-models-handler [request]
  (let [tx (:tx request)
        id (get-in request [:path-params :id])
        p (println ">o> id2" id)
        models-query (-> (sql/select :*)
                         (sql/from :models)
                         (sql/order-by :models.product)

                         (cond-> id (sql/where [:= :models.id [:cast id :uuid]]))

                         (sql/limit 10))
        result (-> models-query
                   sql-format
                   (->> (jdbc/query tx)))]

    (if (nil? id)
      {:body result}
      {:body (first result)}
      )))

(defn create-model-handler [request]
  (let [created_ts (LocalDateTime/now)
        body-params (:body-params request)
        tx (:tx request)
        model body-params
        model (assoc body-params
                     :created_at created_ts
                     :updated_at created_ts)
        p (println ">o> body-params=" model)
        ]
    (try
      (jdbc/insert! tx :models model)
      (response model)
      (catch Exception e
        (error "Failed to create model" e )
        (bad-request {:error "Failed to create model" :details (.getMessage e)})))))

(defn update-model-handler [request]
  (let [model-id (get-in request [:path-params :id])
        body-params (:body-params request)
        tx (:tx request)
        model body-params
        p (println ">o> body-params=" model)]
    (try
      (jdbc/update! tx :models model ["id = ?::uuid" model-id])
      (response {:message "Model updated" :id model-id})
      (catch Exception e
        (error "Failed to update model" e)
        (bad-request {:error "Failed to update model" :details (.getMessage e)})))))

(defn delete-model-handler [request]
  (let [tx (:tx request)
        model-id (get-in request [:path-params :id])]
    (try
      (jdbc/delete! tx :models ["id = ?::uuid" model-id])
      (response {:message "Model deleted" :id model-id})
      (catch Exception e
        (error "Failed to delete model" e)
        (status (bad-request {:error "Failed to delete model"}) 409)))))
