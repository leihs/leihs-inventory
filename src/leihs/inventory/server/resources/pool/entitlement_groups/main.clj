(ns leihs.inventory.server.resources.pool.entitlement-groups.main
  (:require
   [clojure.set]
   [clojure.string]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.entitlement-groups.common :refer [create-entitlements
                                                                            extract-by-keys
                                                                            link-groups-to-entitlement-group
                                                                            link-users-to-entitlement-group]]
   [leihs.inventory.server.resources.pool.entitlement-groups.entitlement-group.query :refer [fetch-users-of-entitlement-group
                                                                                             fetch-groups-of-entitlement-group
                                                                                             enrich-with-is-quantity-ok
                                                                                             fetch-models-of-entitlement-group]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.debug :refer [log-by-severity]]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [leihs.inventory.server.utils.request-utils :refer [body-params path-params]]
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

(defn merge-by-id
  "Merge two vectors of maps by matching :id.
     Fields from v2 override those from v1 on collision."
  [v1 v2]
  (let [m2 (into {} (map (juxt :id identity) v2))]
    (mapv #(merge % (get m2 (:id %))) v1)))

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
                     (if (not (seq models)) []
                         (let [ids (to-uuid (mapv :id models))
                               models (merge-by-id models (enrich-with-is-quantity-ok tx pool_id ids))
                               result (merge-by-id models (enrich-with-stats tx ids))]
                           result)))]
      (response (create-pagination-response request query nil post-fnc)))
    (catch Exception e
      (exception-handler request ERROR_GET e))))

(defn rename-key [m old new]
  (let [old-k (keyword old)
        old-s (name old)
        v (or (get m old-k)
              (get m old-s))]
    (cond-> m
      v (-> (assoc new v)
            (dissoc old-k old-s)))))

(defn post-resource [request]
  (try
    (let [tx (:tx request)
          pool_id (-> request path-params :pool_id)
          data (body-params request)
          models (:models data)
          models (mapv #(rename-key % :id :model_id) models)

          eg-data (extract-by-keys data [:name :is_verification_required])

          entitlement_group (create-entitlement-group tx eg-data pool_id)
          entitlement-group-id (:id entitlement_group)

          _ (link-users-to-entitlement-group tx (->> (:users data)
                                                     (mapv :id)) entitlement-group-id)
          _ (link-groups-to-entitlement-group tx (->> (:groups data)
                                                      (mapv :id)) entitlement-group-id)

          models-with-id (mapv #(assoc % :entitlement_group_id entitlement-group-id) models)
          users (fetch-users-of-entitlement-group tx entitlement-group-id)
          groups (fetch-groups-of-entitlement-group tx entitlement-group-id)]

      (create-entitlements tx models-with-id)
      (let [models-response (fetch-models-of-entitlement-group tx pool_id entitlement-group-id)]
        (response (merge entitlement_group {:models models-response
                                            :users users
                                            :groups groups}))))
    (catch Exception e
      (log-by-severity ERROR_CREATE e)
      (exception-handler request ERROR_CREATE e))))
