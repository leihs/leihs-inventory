(ns leihs.inventory.server.resources.pool.entitlement-groups.entitlement-group.main
  (:require
   [leihs.inventory.server.middlewares.debug :refer [log-by-severity]]
   [leihs.inventory.server.middlewares.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.resources.pool.entitlement-groups.common :refer [create-entitlements
                                                                            delete-entitlements
                                                                            update-entitlements
                                                                            link-users-to-entitlement-group
                                                                            link-groups-to-entitlement-group
                                                                            fetch-entitlements]]
   [leihs.inventory.server.resources.pool.entitlement-groups.entitlement-group.query :refer [analyze-and-prepare-data
                                                                                             update-entitlement-group
                                                                                             fetch-users-of-entitlement-group
                                                                                             fetch-entitlement-group
                                                                                             delete-entitlement-group
                                                                                             fetch-groups-of-entitlement-group
                                                                                             fetch-models-of-entitlement-group]]
   [leihs.inventory.server.utils.request :refer [path-params body-params]]
   [ring.util.response :refer [response status]]))

(def ERROR_GET "Failed to get entitlement-group")
(def ERROR_DELETE "Failed to delete entitlement-group")
(def ERROR_PUT "Failed to update entitlement-group")

(defn- build-response [entitlement-group models]
  (if entitlement-group
    (response (merge entitlement-group {:models models}))
    (status (response {:status "failure" :message "No entry found"}) 404)))

(defn- filter-eg [types coll]
  (->> coll
       (filter (fn [item]
                 (or (nil? types)
                     (contains? types (:type item)))))
       (map #(dissoc % :type))))

(defn- validate-entitlement-group-exists [entitlement-group entitlement-group-id]
  (when (nil? entitlement-group)
    (throw (ex-info "Entitlement group not found"
                    {:entitlement-group-id entitlement-group-id
                     :status 404}))))

(defn- filter-users-by-type [users-raw]
  {:users (filter-eg nil users-raw)
   :direct_users (filter-eg #{"direct_entitlement" "mixed"} users-raw)})

(defn- build-entitlement-response [entitlement-group models users groups]
  (response (merge entitlement-group
                   {:users (:users users)
                    :direct_users (:direct_users users)
                    :groups groups
                    :models models})))

(defn get-resource [request]
  (try
    (let [tx (:tx request)
          entitlement-group-id (-> request path-params :entitlement_group_id)
          entitlement-group (fetch-entitlement-group tx request)]

      (validate-entitlement-group-exists entitlement-group entitlement-group-id)

      (let [models (or (fetch-models-of-entitlement-group tx request) [])
            users-raw (fetch-users-of-entitlement-group tx entitlement-group-id)
            groups (fetch-groups-of-entitlement-group tx entitlement-group-id)
            filtered-users (filter-users-by-type users-raw)]

        (build-entitlement-response entitlement-group models filtered-users groups)))

    (catch Exception e
      (log-by-severity ERROR_GET e)
      (exception-handler request ERROR_GET e))))

(defn delete-resource [request]
  (try
    (let [tx (:tx request)
          entitlement-group-id (-> request path-params :entitlement_group_id)]

      (link-groups-to-entitlement-group tx [] entitlement-group-id)

      (let [{:keys [db-model-ids]} (fetch-entitlements tx entitlement-group-id)
            models (or (delete-entitlements tx db-model-ids entitlement-group-id) [])
            entitlement-group (delete-entitlement-group tx entitlement-group-id)]

        (build-response entitlement-group models)))

    (catch Exception e
      (log-by-severity ERROR_DELETE e)
      (exception-handler request ERROR_DELETE e))))

(defn put-resource [request]
  (try
    (let [tx (:tx request)
          entitlement-group-id (-> request path-params :entitlement_group_id)
          data (-> request body-params)
          eg-data (select-keys data [:name :is_verification_required])]

      (link-users-to-entitlement-group tx (->> (:users data) (map :id)) entitlement-group-id)
      (link-groups-to-entitlement-group tx (->> (:groups data) (map :id)) entitlement-group-id)

      (let [entitlement-group (update-entitlement-group tx eg-data entitlement-group-id)
            {:keys [entitlements-to-update entitlements-to-create entitlement-ids-to-delete]}
            (analyze-and-prepare-data tx (:models data) entitlement-group-id)]

        (update-entitlements tx entitlements-to-update entitlement-group-id)
        (create-entitlements tx entitlements-to-create)
        (delete-entitlements tx entitlement-ids-to-delete entitlement-group-id)

        (let [models-response (or (fetch-models-of-entitlement-group tx request) [])
              users (fetch-users-of-entitlement-group tx entitlement-group-id)
              groups (fetch-groups-of-entitlement-group tx entitlement-group-id)]
          (response (merge entitlement-group {:users users :groups groups :models models-response})))))
    (catch Exception e
      (log-by-severity ERROR_PUT e)
      (exception-handler request ERROR_PUT e))))
