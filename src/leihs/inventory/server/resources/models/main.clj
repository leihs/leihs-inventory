(ns leihs.inventory.server.resources.models.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [next.jdbc.sql :as jdbc]
   [ring.util.response :refer [response bad-request]]))

(def models-query
  (-> (sql/select :*)
      (sql/from :models)
      (sql/order-by :models.product)
      (sql/limit 10)))

(defn models [{tx :tx}]
  {:body
   (-> models-query
       sql-format
       (->> (jdbc/query tx)))})

(defn get-models-handler [request]
  (case (:request-method request)
    :get (models request)))

(defn create-model-handler [{:keys [tx body-params]}]
  (let [model {:product (get body-params :product)
               :manufacturer (get body-params :manufacturer)}]
    (try
      (jdbc/insert! tx :models model)
      (response {:message "Model created" :model model})
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
