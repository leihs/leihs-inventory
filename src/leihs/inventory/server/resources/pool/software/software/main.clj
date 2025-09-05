(ns leihs.inventory.server.resources.pool.software.software.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.common :refer [fetch-attachments
                                                         is-model-deletable?
                                                         str-to-bool]]
   [leihs.inventory.server.resources.pool.models.common :refer [filter-map-by-spec]]
   [leihs.inventory.server.resources.pool.models.helper :refer [normalize-model-data]]
   [leihs.inventory.server.resources.pool.models.model.main :refer [db-operation
                                                                    filter-keys]]
   [leihs.inventory.server.resources.pool.software.software.types :as types]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.helper :refer [log-by-severity]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [not-found response]])
  (:import
   (java.time LocalDateTime)))

(def ERROR_DELETE_SOFTWARE "Failed to delete software")
(def ERROR_UPDATE_SOFTWARE "Failed to update software")
(def ERROR_FETCH_SOFTWARE "Failed to fetch software")

(defn get-resource [request]
  (try
    (let [tx (get-in request [:tx])
          model-id (to-uuid (get-in request [:path-params :model_id]))
          pool-id (to-uuid (get-in request [:path-params :pool_id]))
          model-query (-> (sql/select :m.id :m.product :m.manufacturer :m.version :m.type
                                      :m.hand_over_note :m.description :m.internal_description
                                      :m.technical_detail :m.is_package)
                          (sql/from [:models :m])
                          (sql/where [:and [:= :m.id model-id] [:= :m.type "Software"]])
                          sql-format)
          model-result (jdbc/execute-one! tx model-query)
          result (when model-result (let [model-result (assoc model-result :is_deletable (is-model-deletable? tx model-id "Software"))
                                          attachments (fetch-attachments tx model-id pool-id)
                                          result (assoc model-result :attachments attachments)] result))]
      (if result
        (response (filter-map-by-spec result ::types/put-response))
        (not-found {:message ERROR_FETCH_SOFTWARE})))
    (catch Exception e
      (log-by-severity ERROR_FETCH_SOFTWARE e)
      (exception-handler ERROR_FETCH_SOFTWARE e))))

(defn prepare-software-data [data]
  (let [normalize-data (normalize-model-data data)
        created-ts (LocalDateTime/now)]
    (assoc normalize-data :updated_at created-ts :is_package (str-to-bool (:is_package normalize-data)))))

(defn put-resource [request]
  (try
    (let [model-id (to-uuid (get-in request [:path-params :model_id]))
          multipart (get-in request [:parameters :body])
          tx (:tx request)
          prepared-model-data (prepare-software-data multipart)
          update-model-query (-> (sql/update [:models :m])
                                 (sql/set prepared-model-data)
                                 (sql/where [:and [:= :m.id model-id] [:= :m.type "Software"]])
                                 (sql/returning :*)
                                 sql-format)
          updated-model (jdbc/execute-one! tx update-model-query)]

      (if updated-model
        (response (filter-map-by-spec updated-model ::types/put-response))
        (not-found {:message ERROR_UPDATE_SOFTWARE})))
    (catch Exception e
      (log-by-severity ERROR_UPDATE_SOFTWARE e)
      (exception-handler ERROR_UPDATE_SOFTWARE e))))

(defn delete-resource [request]
  (try
    (let [model-id (to-uuid (get-in request [:path-params :model_id]))
          tx (:tx request)
          is-model-deletable? (is-model-deletable? tx model-id "Software")
          where-clause-model [:and [:= :id model-id] [:= :type "Software"]]
          models (db-operation tx :select :models where-clause-model)]

      (if (seq models)
        (if is-model-deletable?
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
          (throw (ex-info "Request to delete software blocked: software in use" {:status 409})))
        (throw (ex-info "Request to delete software blocked: software not found" {:status 404}))))
    (catch Exception e
      (log-by-severity ERROR_DELETE_SOFTWARE e)
      (exception-handler ERROR_DELETE_SOFTWARE e))))
