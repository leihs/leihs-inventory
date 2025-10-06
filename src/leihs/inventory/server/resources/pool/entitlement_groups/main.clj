(ns leihs.inventory.server.resources.pool.entitlement-groups.main
  (:require
   [clojure.set]
   [clojure.string]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.entitlement-groups.common :refer [create-entitlements
                                                                            link-users-to-entitlement-group
                                                                            link-groups-to-entitlement-group]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [leihs.inventory.server.utils.request-utils :refer [path-params body-params]]
   [next.jdbc :as jdbc]
   [leihs.inventory.server.utils.debug :refer [log-by-severity]]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.request-utils :refer [path-params]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [response]]))

(defn- prep-query [ids]
  (sql-format
   {:raw
    (str
     "SELECT
               eg.id,
               eg.name,
               eg.is_verification_required,
               COALESCE(m.number_of_models, 0) AS number_of_models,
               COALESCE(u.number_of_users, 0) AS number_of_users,
               COALESCE(u.number_of_direct_users, 0) AS number_of_direct_users,
               COALESCE(g.number_of_groups, 0) AS number_of_groups
            FROM entitlement_groups eg
            LEFT JOIN (
                SELECT entitlement_group_id, COUNT(id) AS number_of_models
                FROM entitlements
                GROUP BY entitlement_group_id
            ) m ON m.entitlement_group_id = eg.id
            LEFT JOIN (
                SELECT entitlement_group_id,
                       COUNT(id) AS number_of_users,
                       SUM(CASE WHEN type IN ('direct_entitlement', 'mixed')
                                THEN 1 ELSE 0 END) AS number_of_direct_users
                FROM entitlement_groups_users
                GROUP BY entitlement_group_id
            ) u ON u.entitlement_group_id = eg.id
            LEFT JOIN (
                SELECT entitlement_group_id, COUNT(id) AS number_of_groups
                FROM entitlement_groups_groups
                GROUP BY entitlement_group_id
            ) g ON g.entitlement_group_id = eg.id
            WHERE eg.id IN ("
     (->> ids
          (map #(str "'" % "'"))
          (clojure.string/join ", "))
     ");")}))

(def ERROR_GET "Failed to get entitlement-groups")

(defn- merge-by-id
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
          query (-> (sql/select :g.*)
                    (sql/from [:entitlement_groups :g])
                    (sql/join [:inventory_pools :ip] [:= :g.inventory_pool_id :ip.id])
                    (cond-> pool_id (sql/where [:= :g.inventory_pool_id pool_id]))
                    (sql/order-by :g.name))
          post-fnc (fn [models]
                     (if (seq models)
                       (let [ids (to-uuid (mapv :id models))
                             result (jdbc/execute! tx (prep-query ids))]
                         (merge-by-id models result))
                       []))]
      (response (create-pagination-response request query nil post-fnc)))
    (catch Exception e
      (exception-handler request ERROR_GET e))))

(defn post-resource [request]
  (try
    (let [tx (:tx request)
          pool_id (-> request path-params :pool_id)
          data (body-params request)
          models (:models data)
          entitlement_group (create-entitlement-group tx (:entitlement_group data) pool_id)

          users (link-users-to-entitlement-group tx (:users data) (:id entitlement_group))
          groups (link-groups-to-entitlement-group tx (:groups data) (:id entitlement_group))

          entitlement_group_id (:id entitlement_group)
          models-with-id (mapv #(assoc % :entitlement_group_id entitlement_group_id) models)
          created-entitlements (create-entitlements tx models-with-id)]
      (response {:entitlement_group entitlement_group
                 :models created-entitlements
                 :users users
                 :groups groups}))
    (catch Exception e
      (exception-handler request ERROR_GET e))))
