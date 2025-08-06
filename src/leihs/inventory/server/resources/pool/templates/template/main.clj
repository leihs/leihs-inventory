(ns leihs.inventory.server.resources.pool.templates.template.main
  (:require
   [clojure.set]
   [clojure.string :as str]
   [honey.sql :as sq]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.models.model.main :refer [db-operation]]
   [leihs.inventory.server.resources.pool.templates.common :refer [analyze-datasets
                                                                   fetch-template-with-models
                                                                   process-create-template-models
                                                                   process-update-template-models
                                                                   process-delete-template-models]]
   [leihs.inventory.server.resources.pool.templates.template.types :as types]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request not-found response status]]
   [taoensso.timbre :refer [debug error]]))

(def ERROR_DELETION "Failed to delete template")
(def ERROR_FETCH "Failed to fetch template")
(def ERROR_UPDATE "Failed to update software")

(defn get-resource [request]
  (let [tx (:tx request)
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        template-id (to-uuid (get-in request [:path-params :template_id]))
        templates (fetch-template-with-models tx template-id pool-id)]
    (try
      (if templates
        (response templates)
        (not-found {:error ERROR_FETCH}))
      (catch Exception e
        (error ERROR_FETCH (.getMessage e))
        (bad-request {:error ERROR_FETCH :details (.getMessage e)})))))

(defn put-resource [request]
  (try
    (let [tx (:tx request)
          template-id (to-uuid (get-in request [:path-params :template_id]))
          pool-id (to-uuid (get-in request [:path-params :pool_id]))
          {:keys [name models]} (get-in request [:parameters :body])
          template-data (fetch-template-with-models tx template-id pool-id)
          analyzed-datasets (analyze-datasets (:models template-data) models)
          entries-to-delete (:missing-in-new-data analyzed-datasets)
          entries-to-update (:different-quantity analyzed-datasets)
          entries-to-insert (:new-entries analyzed-datasets)]

      (debug "process: update template")
      (when (not= (:name template-data) name)
        (jdbc/execute! tx (-> (sql/update :model_groups)
                              (sql/set {:name name})
                              (sql/where [:and [:= :id template-id]
                                          [:= :type "Template"]])
                              sql-format)))

      (process-delete-template-models tx entries-to-delete)
      (process-update-template-models tx entries-to-update)
      (process-create-template-models tx entries-to-insert template-id pool-id)

      (if-let [templates (fetch-template-with-models tx template-id pool-id)]
        (response templates)
        (not-found {:error ERROR_UPDATE})))
    (catch Exception e
      (error ERROR_UPDATE e)
      (cond
        (str/includes? (.getMessage e) "violates")
        (-> (response {:status "failure"
                       :message ERROR_UPDATE
                       :detail (.getMessage e)})
            (status 409))
        :else (bad-request {:error ERROR_UPDATE :details (.getMessage e)})))))

(defn delete-resource [request]
  (try
    (let [pool-id (to-uuid (get-in request [:path-params :pool_id]))
          template-id (to-uuid (get-in request [:path-params :template_id]))
          tx (:tx request)
          template (db-operation tx :select :model_groups [:and [:= :id template-id] [:= :type "Template"]])]

      (if (= 1 (count template))
        (let [deleted-ipmg (db-operation tx :delete :inventory_pools_model_groups
                                         [:and [:= :inventory_pool_id pool-id]
                                          [:= :model_group_id template-id]])
              deleted-template (db-operation tx :delete :model_groups [:= :id template-id])]
          (if (= 1 (count deleted-template))
            (response {:deleted_ipmg deleted-ipmg
                       :deleted_template deleted-template})
            (throw (ex-info "Template not found" {:status 404}))))
        (throw (ex-info "Template not found" {:status 404}))))
    (catch Exception e
      (error ERROR_DELETION e)
      (cond
        (str/includes? (.getMessage e) "violates")
        (-> (response {:status "failure"
                       :message ERROR_DELETION
                       :detail (.getMessage e)})
            (status 409))
        :else (bad-request {:error ERROR_DELETION :details (.getMessage e)})))))
