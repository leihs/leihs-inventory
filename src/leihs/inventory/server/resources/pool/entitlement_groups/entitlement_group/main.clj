(ns leihs.inventory.server.resources.pool.entitlement-groups.entitlement-group.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.entitlement-groups.common :refer [create-entitlements
                                                                            delete-entitlements
                                                                            update-entitlements
                                                                            link-users-to-entitlement-group
                                                                            link-groups-to-entitlement-group
                                                                            fetch-entitlements]]
   [leihs.inventory.server.resources.pool.entitlement-groups.entitlement-group.query :refer [analyze-and-prepare-data
                                                                                             update-entitlement-groups
                                                                                             fetch-users-of-entitlement-group
                                                                                             fetch-entitlement-group
                                                                                             fetch-groups-of-entitlement-group
                                                                                             fetch-models-of-entitlement-group]]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.request-utils :refer [path-params body-params]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [response status]]
   [taoensso.timbre :refer [error]]))

(def ERROR_GET "Failed to get entitlement-groups")

(defn- verify-unique-entries! [request]
  (let [models (-> request body-params :mdoels)
        ids (map :model_id models)
        duplicates (->> ids
                        frequencies
                        (filter (fn [[_ freq]] (> freq 1)))
                        (map first))]
    (when (seq duplicates)
      (throw (ex-info "Duplicate model_id(s) detected"
                      {:duplicate-model-ids duplicates
                       :status 400})))))

(defn get-resource [request]
  (try
    (let [tx (:tx request)
          entitlement-group-id (-> request path-params :entitlement_group_id)
          entitlement-group (fetch-entitlement-group tx request)
          users-groups (fetch-users-of-entitlement-group tx entitlement-group-id)
          models (fetch-models-of-entitlement-group tx request)
          groups (fetch-groups-of-entitlement-group tx entitlement-group-id)]
      (response {:entitlement_group entitlement-group
                 :users users-groups
                 :groups groups
                 :models models}))
    (catch Exception e
      (error e "Error fetching entitlement group")
      (exception-handler request ERROR_GET e))))

(defn delete-resource [request]
  (try
    (let [tx (:tx request)
          entitlement-group-id (-> request path-params :entitlement_group_id)
          {:keys [db-model-ids]} (fetch-entitlements tx entitlement-group-id)
          models (delete-entitlements tx db-model-ids)
          result (jdbc/execute-one! tx (-> (sql/delete-from :entitlement_groups)
                                           (sql/where [:= :id entitlement-group-id])
                                           (sql/returning :*)
                                           sql-format))]
      (if result
        (response {:entitlement_groups result
                   :models models})
        (status (response {:status "failure" :message "No entry found"}) 404)))

    (catch Exception e
      (error e "Error deleting entitlement group")
      (exception-handler request ERROR_GET e))))

(defn put-resource [request]
  (try
    (verify-unique-entries! request)
    (let [tx (:tx request)
          entitlement-group-id (-> request path-params :entitlement_group_id)
          data (-> request body-params)
          entitlement-group (:entitlement_group data)
          models (:models data)
          users (:users data)

          users-status (link-users-to-entitlement-group tx users entitlement-group-id)
          groups-status (link-groups-to-entitlement-group tx (:groups data) entitlement-group-id)
          entitlement-group (update-entitlement-groups tx entitlement-group entitlement-group-id)

          {:keys [entitlements-to-update entitlements-to-create entitlement-ids-to-delete]}
          (analyze-and-prepare-data tx models entitlement-group-id)

          updated-entitlements (update-entitlements tx entitlements-to-update)
          created-entitlements (create-entitlements tx entitlements-to-create)
          deleted-entitlements (delete-entitlements tx entitlement-ids-to-delete)]

      (response {:entitlement_group entitlement-group
                 :users users-status
                 :groups groups-status
                 :models {:updated updated-entitlements
                          :created created-entitlements
                          :deleted deleted-entitlements}}))
    (catch Exception e
      (println e)
      (exception-handler request ERROR_GET e))))
