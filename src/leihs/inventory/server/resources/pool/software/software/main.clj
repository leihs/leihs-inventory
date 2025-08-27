(ns leihs.inventory.server.resources.pool.software.software.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.common :refer [str-to-bool fetch-attachments]]
   [leihs.inventory.server.resources.pool.models.common :refer [filter-map-by-spec]]
   [leihs.inventory.server.resources.pool.models.helper :refer [normalize-model-data]]
   [leihs.inventory.server.resources.pool.models.model.main :refer [db-operation filter-keys]]
   [leihs.inventory.server.resources.pool.software.software.types :as types]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response not-found]]
   [taoensso.timbre :refer [debug error]])
  (:import
   (java.time LocalDateTime)))

(defn get-resource [request]
  (let [tx (get-in request [:tx])
        model-id (to-uuid (get-in request [:path-params :model_id]))
        pool-id (to-uuid (get-in request [:path-params :pool_id]))]
    (try
      (let [model-query (-> (sql/select :m.id :m.product :m.manufacturer :m.version :m.type
                                        :m.hand_over_note :m.description :m.internal_description
                                        :m.technical_detail :m.is_package)
                            (sql/from [:models :m])
                            (sql/where [:and [:= :m.id model-id] [:= :m.type "Software"]])
                            sql-format)
            model-result (jdbc/execute-one! tx model-query)
            result (when model-result (let [attachments (fetch-attachments tx model-id pool-id)
                                            result (assoc model-result :attachments attachments)] result))]
        (if result
          (response (filter-map-by-spec result ::types/put-response))
          (not-found {:error "Failed to fetch software"})))
      (catch Exception e
        (debug e)
        (error "Failed to fetch software" (.getMessage e))
        (bad-request {:error "Failed to fetch software" :details (.getMessage e)})))))

(defn prepare-software-data [data]
  (let [normalize-data (normalize-model-data data)
        created-ts (LocalDateTime/now)]
    (assoc normalize-data :updated_at created-ts :is_package (str-to-bool (:is_package normalize-data)))))

(defn put-resource [request]
  (let [model-id (to-uuid (get-in request [:path-params :model_id]))
        multipart (get-in request [:parameters :body])
        tx (:tx request)
        prepared-model-data (prepare-software-data multipart)]
    (try
      (let [update-model-query (-> (sql/update [:models :m])
                                   (sql/set prepared-model-data)
                                   (sql/where [:and [:= :m.id model-id] [:= :m.type "Software"]])
                                   (sql/returning :*)
                                   sql-format)
            updated-model (jdbc/execute-one! tx update-model-query)]

        (if updated-model
          (response (filter-map-by-spec updated-model ::types/put-response))
          (not-found {:error "Failed to update software"})))
      (catch Exception e
        (debug e)
        (error "Failed to update software" (.getMessage e))
        (bad-request {:error "Failed to update software" :details (.getMessage e)})))))

(defn delete-resource [request]
  (let [model-id (to-uuid (get-in request [:path-params :model_id]))
        tx (:tx request)
        where-clause-model [:and [:= :id model-id] [:= :type "Software"]]
        models (db-operation tx :select :models where-clause-model)]

    (if (seq models)
      (let [attachments (db-operation tx :select :attachments [:= :model_id model-id])
            deleted-model (jdbc/execute! tx (-> (sql/delete-from :models)
                                                (sql/where where-clause-model)
                                                (sql/returning :*)
                                                sql-format))
            result {:deleted_attachments (filter-keys attachments [:id :model_id :filename :size])
                    :deleted_model (filter-keys deleted-model [:id :product :manufacturer])}]
        (if (= 1 (count deleted-model))
          (response result)
          (throw (ex-info "Request to delete software failed" {:status 409}))))
      (throw (ex-info "Request to delete software blocked: software not found" {:status 404})))))
