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
                                                                            extract-by-keys
                                                                            fetch-entitlements]]
   [leihs.inventory.server.resources.pool.entitlement-groups.entitlement-group.query :refer [analyze-and-prepare-data
                                                                                             update-entitlement-group
                                                                                             fetch-users-of-entitlement-group
                                                                                             fetch-entitlement-group
                                                                                             fetch-groups-of-entitlement-group
                                                                                             fetch-models-of-entitlement-group]]
   [leihs.inventory.server.utils.debug :refer [log-by-severity]]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.request-utils :refer [path-params body-params]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [response status]]
   [taoensso.timbre :refer [error]]))

(def ERROR_GET "Failed to get entitlement-group")
(def ERROR_DELETE "Failed to delete entitlement-group")
(def ERROR_PUT "Failed to update entitlement-group")

(defn- verify-unique-entries! [request]
  (let [models (-> request body-params :models)
        ids (map :id models)
        duplicates (->> ids
                        frequencies
                        (filter (fn [[_ freq]] (> freq 1)))
                        (map first))]
    (when (seq duplicates)
      (throw (ex-info "Duplicate model_id(s) detected"
                      {:duplicate-model-ids duplicates
                       :status 400})))))

(defn- filter-eg [type coll]
  (->> coll
       (filter (fn [item]
                 (or (nil? type)
                     (= type (:type item)))))
       (map #(dissoc % :type))))

(defn get-resource [request]
  (try
    (let [tx (:tx request)
          entitlement-group-id (-> request path-params :entitlement_group_id)
          entitlement-group (fetch-entitlement-group tx request)

          _ (when (nil? entitlement-group)
              (throw (ex-info "Entitlement group not found"
                              {:entitlement-group-id entitlement-group-id
                               :status 404})))

          models (fetch-models-of-entitlement-group tx request)
          users-raw (fetch-users-of-entitlement-group tx entitlement-group-id)
          groups (fetch-groups-of-entitlement-group tx entitlement-group-id)]
      (response (merge entitlement-group {:users (filter-eg nil users-raw)
                                          :direct_users (filter-eg "direct_entitlement" users-raw)
                                          :groups groups
                                          :models models})))
    (catch Exception e
      (error e "Error fetching entitlement group")
      (exception-handler request ERROR_GET e))))

(defn delete-resource [request]
  (try
    (let [tx (:tx request)
          entitlement-group-id (-> request path-params :entitlement_group_id)
          _ (link-groups-to-entitlement-group tx [] entitlement-group-id)
          {:keys [db-model-ids]} (fetch-entitlements tx entitlement-group-id)
          models (delete-entitlements tx db-model-ids entitlement-group-id)
          result (jdbc/execute-one! tx (-> (sql/delete-from :entitlement_groups)
                                           (sql/where [:= :id entitlement-group-id])
                                           (sql/returning :*)
                                           sql-format))]
      (if result
        (response (merge result {:models models}))
        (status (response {:status "failure" :message "No entry found"}) 404)))

    (catch Exception e
      (error e ERROR_DELETE)
      (exception-handler request ERROR_DELETE e))))

(defn put-resource [request]
  (try
    (verify-unique-entries! request)
    (let [tx (:tx request)
          entitlement-group-id (-> request path-params :entitlement_group_id)
          data (-> request body-params)
          eg-data (extract-by-keys data [:name :is_verification_required])
          models (:models data)

          _ (link-users-to-entitlement-group tx (->> (:users data)
                                                     (mapv :id)) entitlement-group-id)
          _ (link-groups-to-entitlement-group tx (->> (:groups data)
                                                      (mapv :id)) entitlement-group-id)

          entitlement-group (update-entitlement-group tx eg-data entitlement-group-id)

          {:keys [entitlements-to-update entitlements-to-create entitlement-ids-to-delete]}
          (analyze-and-prepare-data tx models entitlement-group-id)

          _ (update-entitlements tx entitlements-to-update entitlement-group-id)
          _ (create-entitlements tx entitlements-to-create)
          _ (delete-entitlements tx entitlement-ids-to-delete entitlement-group-id)

          models-response (fetch-models-of-entitlement-group tx request)
          users (fetch-users-of-entitlement-group tx entitlement-group-id)
          groups (fetch-groups-of-entitlement-group tx entitlement-group-id)]

      (response (merge entitlement-group {:users users
                                          :groups groups
                                          :models models-response})))
    (catch Exception e
      (log-by-severity ERROR_PUT e)
      (exception-handler request ERROR_PUT e))))
