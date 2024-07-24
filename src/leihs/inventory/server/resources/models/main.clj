(ns leihs.inventory.server.resources.models.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [next.jdbc.sql :as jdbc]
  [taoensso.timbre :refer [error]]
   [ring.util.response :refer [bad-request response status]])

(:import (java.time LocalDateTime)))

;(def models-query
;  (-> (sql/select :*)
;      (sql/from :models)
;      (sql/order-by :models.product)
;      (sql/limit 10)))
;
;(defn models [{tx :tx}]
;  {:body
;   (-> models-query
;       sql-format
;       (->> (jdbc/query tx)))})
;
;(defn get-models-handler [request]
;  (case (:request-method request)
;    :get (models request)))

;(defn get-models-handler [{:keys [tx request-method]}]
(defn get-models-handler [request]
  ;(case request-method
  ;  :get
  (let [
        tx (:tx request)
        ;id (Integer. (get-in request [:path-params "id"]))
        ;id (get-in request [:path-params "id"])
        ;p (println ">o> id=" id)
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
      )

    ))



;(defn create-model-handler [{:keys [tx body-params]}]
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
      ;(response {:message "Model created" :model model})
      (response model)
      (catch Exception e
        (bad-request {:error "Failed to create model" :details (.getMessage e)})))))

;(defn update-model-handler [{:keys [tx path-params body-params]}]
(defn update-model-handler [request]
  (let [
        ;model-id (Integer. (get path-params "id"))
        ;updated-fields (select-keys body-params [:product :manufacturer])


        model-id (get-in request [:path-params :id])

        body-params (:body-params request)
        tx (:tx request)
        model body-params
        ;model (assoc body-params
        ;             :created_at created_ts
        ;             :updated_at created_ts)
        p (println ">o> body-params=" model)

                ]
    (try
      ;(jdbc/update! tx :models model {:id model-id})
      ;(jdbc/update! tx :models model {:id [:cast model-id :uuid]})

      (jdbc/update! tx :models model ["id = ?::uuid" model-id])




      (response {:message "Model updated" :id model-id})
      (catch Exception e
        (bad-request {:error "Failed to update model" :details (.getMessage e)})))))

;(defn delete-model-handler [{:keys [tx path-params]}]
(defn delete-model-handler [request]
  (let [
        tx (:tx request)
        ;model-id (Integer. (get path-params "id"))
        model-id (get-in request [:path-params :id])

        ]
    (try
      ;(jdbc/delete! tx :models {:id model-id})
      (jdbc/delete! tx :models  ["id = ?::uuid" model-id])

      (response {:message "Model deleted" :id model-id})
      (catch Exception e
        ;(bad-request {:error "Failed to delete model" :details (.getMessage e)})))))
        ;(status (bad-request {:error "Failed to delete model" :details (.getMessage e)}) 409)))))
        (error "Failed to delete model" e)
        (status (bad-request {:error "Failed to delete model"}) 409)))))
        ;(status {:error "Failed to delete model" :details (.getMessage e)} 409)))))
