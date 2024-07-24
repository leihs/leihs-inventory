(ns leihs.inventory.server.resources.models.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [next.jdbc.sql :as jdbc]
   [ring.util.response :refer [bad-request response]])
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
                         ;(when id (sql/where := :models.id id) )
                         (cond-> id (sql/where [:= :models.id id]))

                         (sql/limit 10))
        result (-> models-query
                   sql-format
                   (->> (jdbc/query tx)))]

    ;(if (nil? id)
    ;  {:body result}
    ;  {:body (first result)}
    ;  )

      {:body result}

    ;))
    ;)
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

(defn update-model-handler [{:keys [tx path-params body-params]}]
  (let [model-id (Integer. (get path-params "id"))
        updated-fields (select-keys body-params [:product :manufacturer])]
    (try
      (jdbc/update! tx :models updated-fields {:id model-id})
      (response {:message "Model updated" :id model-id})
      (catch Exception e
        (bad-request {:error "Failed to update model" :details (.getMessage e)})))))

(defn delete-model-handler [{:keys [tx path-params]}]
  (let [model-id (Integer. (get path-params "id"))]
    (try
      (jdbc/delete! tx :models {:id model-id})
      (response {:message "Model deleted" :id model-id})
      (catch Exception e
        (bad-request {:error "Failed to delete model" :details (.getMessage e)})))))
