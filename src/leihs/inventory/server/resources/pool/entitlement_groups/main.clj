(ns leihs.inventory.server.resources.pool.entitlement-groups.main
  (:require
   [clojure.set :as set]
   [clojure.string]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.middlewares.debug :refer [log-by-severity]]
   [leihs.inventory.server.middlewares.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.resources.pool.entitlement-groups.common :refer [create-entitlements
                                                                            link-groups-to-entitlement-group
                                                                            link-users-to-entitlement-group]]
   [leihs.inventory.server.resources.pool.entitlement-groups.entitlement-group.query :refer [fetch-users-of-entitlement-group
                                                                                             fetch-groups-of-entitlement-group
                                                                                             enrich-with-is-quantity-ok
                                                                                             fetch-models-of-entitlement-group]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [leihs.inventory.server.utils.request :refer [body-params path-params]]
   [leihs.inventory.server.utils.transform :refer [merge-by-id]]
   [next.jdbc :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [response]]))

(def ERROR_GET "Failed to get entitlement-groups")
(def ERROR_CREATE "Failed to create entitlement-groups")

(defn- enrich-with-stats [tx ids]
  (let [m-subquery (-> (sql/select :entitlement_group_id
                                   [[:count :id] :number_of_models])
                       (sql/from :entitlements)
                       (sql/group-by :entitlement_group_id))
        u-subquery (-> (sql/select :entitlement_group_id
                                   [[:count :id] :number_of_users]
                                   [[:sum [:cast
                                           [:case
                                            [:in :type ["direct_entitlement" "mixed"]] 1
                                            :else 0]
                                           :integer]]
                                    :number_of_direct_users])
                       (sql/from :entitlement_groups_users)
                       (sql/group-by :entitlement_group_id))
        g-subquery (-> (sql/select :entitlement_group_id
                                   [[:count :id] :number_of_groups])
                       (sql/from :entitlement_groups_groups)
                       (sql/group-by :entitlement_group_id))
        query (-> (sql/select :eg.id
                              :eg.name
                              :eg.is_verification_required
                              [[:coalesce :m.number_of_models 0] :number_of_models]
                              [[:coalesce :u.number_of_users 0] :number_of_users]
                              [[:coalesce :u.number_of_direct_users 0] :number_of_direct_users]
                              [[:coalesce :g.number_of_groups 0] :number_of_groups])
                  (sql/from [:entitlement_groups :eg])
                  (sql/left-join [m-subquery :m] [:= :m.entitlement_group_id :eg.id])
                  (sql/left-join [u-subquery :u] [:= :u.entitlement_group_id :eg.id])
                  (sql/left-join [g-subquery :g] [:= :g.entitlement_group_id :eg.id])
                  (sql/where [:in :eg.id ids])
                  sql-format)]
    (jdbc/execute! tx query)))

(defn- create-entitlement-group [tx data pool_id]
  (let [current-time (java.sql.Timestamp/from (java.time.Instant/now))
        new-eg (-> data
                   (assoc :inventory_pool_id (to-uuid pool_id) :created_at current-time :updated_at current-time))
        query (-> (sql/insert-into :entitlement_groups)
                  (sql/values [new-eg])
                  (sql/returning :*)
                  sql-format)]
    (jdbc/execute-one! tx query)))

(defn index-resources [request]
  (try
    (let [tx (:tx request)
          pool_id (-> request path-params :pool_id)
          query (-> (sql/select :g.* [[:coalesce [:sum :e.quantity] 0] :number_of_allocations])
                    (sql/from [:entitlement_groups :g])
                    (sql/join [:inventory_pools :ip] [:= :g.inventory_pool_id :ip.id])
                    (sql/left-join [:entitlements :e] [:= :e.entitlement_group_id :g.id])
                    (cond-> pool_id (sql/where [:= :g.inventory_pool_id pool_id]))
                    (sql/group-by :g.id)
                    (sql/order-by :g.name :g.id))

          post-fnc (fn [models]
                     (or (when (seq models)
                           (let [ids (to-uuid (map :id models))
                                 models (merge-by-id models (enrich-with-is-quantity-ok tx pool_id ids))
                                 result (merge-by-id models (enrich-with-stats tx ids))]
                             result))
                         []))]
      (response (create-pagination-response request query nil post-fnc)))
    (catch Exception e
      (exception-handler request ERROR_GET e))))

(defn- prepare-models [models entitlement-group-id]
  (->> models
       (map #(assoc % :entitlement_group_id entitlement-group-id))))

(defn- link-entities [tx data entitlement-group-id]
  (when-let [user-ids (seq (map :id (:users data)))]
    (link-users-to-entitlement-group tx user-ids entitlement-group-id))
  (when-let [group-ids (seq (map :id (:groups data)))]
    (link-groups-to-entitlement-group tx group-ids entitlement-group-id)))

(defn- fetch-related-entities [tx pool-id entitlement-group-id]
  (let [users (fetch-users-of-entitlement-group tx entitlement-group-id)
        groups (fetch-groups-of-entitlement-group tx entitlement-group-id)
        models (or (fetch-models-of-entitlement-group tx pool-id entitlement-group-id) [])]
    {:users users
     :groups groups
     :models models}))

(defn- build-response [entitlement-group related-entities]
  (response (merge entitlement-group related-entities)))

(defn post-resource [request]
  (try
    (let [tx (:tx request)
          pool-id (-> request path-params :pool_id)
          data (body-params request)
          models (map #(set/rename-keys % {:id :model_id}) (:models data))
          eg-data (select-keys data [:name :is_verification_required])
          entitlement-group (create-entitlement-group tx eg-data pool-id)
          entitlement-group-id (:id entitlement-group)
          prepared-models (prepare-models models entitlement-group-id)]

      (link-entities tx data entitlement-group-id)
      (create-entitlements tx prepared-models)

      (let [related-entities (fetch-related-entities tx pool-id entitlement-group-id)]
        (build-response entitlement-group related-entities)))

    (catch Exception e
      (log-by-severity ERROR_CREATE e)
      (exception-handler request ERROR_CREATE e))))
