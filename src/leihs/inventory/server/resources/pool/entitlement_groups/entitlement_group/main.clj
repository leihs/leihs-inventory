(ns leihs.inventory.server.resources.pool.entitlement-groups.entitlement-group.main
  (:require
   [clojure.set]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.request-utils :refer [path-params]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [response]]
   [taoensso.timbre :refer [debug error]])
  (:import [java.util UUID]))

;; -----------------------------
;; Utility functions
;; -----------------------------

(defn to-uuid [x]
  (cond
    (nil? x) nil
    (uuid? x) x
    (string? x) (UUID/fromString x)
    (sequential? x) (mapv to-uuid x)
    :else (throw (ex-info "Unsupported type for uuid conversion"
                   {:value x :type (type x)}))))

(def ERROR_GET "Failed to get entitlement-groups")

(defn merge-by-id
  "Merge two vectors of maps by matching :id.
   Fields from v2 override those from v1 on collision."
  [v1 v2]
  (let [m2 (into {} (map (juxt :id identity) v2))]
    (mapv #(merge % (get m2 (:id %))) v1)))

(defn join-by
  "Left-join two collections of maps by shared key k.
   Keeps all maps from coll-a, merging matches from coll-b."
  [k coll-a coll-b]
  (let [b-index (into {} (map (juxt k identity)) coll-b)]
    (mapv #(merge % (get b-index (get % k))) coll-a)))

(defn add-allocation-considered-count
  "Adds computed fields:
   :available_count = items_count - allocations_in_other_entitlement_groups
   :is_quantity_ok = quantity <= available_count
   Also removes some transient keys."
  [entitlements]
  (mapv (fn [e]
          (let [available (- (:items_count e 0)
                            (:allocations_in_other_entitlement_groups e 0))
                e (assoc e
                    :available_count available
                    :is_quantity_ok (<= (:quantity e 0) available))]
            (dissoc e :entitlement_group_id :allocations_in_other_entitlement_groups)))
    entitlements))

;; -----------------------------
;; Queries
;; -----------------------------

(defn prep-query [ids]
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
       (->> ids (map #(str "'" % "'")) (str/join ", "))
       ");")}))

(defn select-entitlements-with-item-count
  "Selects entitlements with corresponding item counts for given pool and models."
  [ds inventory-pool-id model-ids exclude-group-id]
  (let [subquery
        {:select [[[:count :*] :count]]
         :from [[:items :i]]
         :where [:and
                 [:= :i.model_id :e.model_id]
                 [:= :i.inventory_pool_id :eg.inventory_pool_id]
                 [:is :i.retired nil]
                 [:= :i.is_borrowable true]
                 [:is :i.parent_id nil]]}

        query (-> (sql/select
                    :e.model_id
                    [:e.quantity :allocations_in_other_entitlement_groups]
                    [[subquery] :items_count])
                (sql/from [:entitlements :e])
                (sql/join [:entitlement_groups :eg]
                  [:= :eg.id :e.entitlement_group_id])
                (sql/where [:and
                            [:= :eg.inventory_pool_id inventory-pool-id]
                            [:in :e.model_id model-ids]
                            [:!= :e.entitlement_group_id exclude-group-id]])
                sql-format)]
    (jdbc/execute! ds query)))

;; -----------------------------
;; HTTP Resource Handler
;; -----------------------------

(defn get-resource [request]
  (try
    (let [tx (:tx request)
          pool-id (-> request path-params :pool_id)
          entitlement-group-id (-> request path-params :entitlement_group_id)

          ;; 1️⃣ Fetch entitlement group
          query (-> (sql/select :g.id :g.name :g.is_verification_required)
                  (sql/from [:entitlement_groups :g])
                  (sql/join [:inventory_pools :ip] [:= :g.inventory_pool_id :ip.id])
                  (sql/where [:and
                              [:= :g.inventory_pool_id pool-id]
                              [:= :g.id entitlement-group-id]])
                  (sql/order-by :g.name)
                  sql-format)
          entitlement-group (jdbc/execute-one! tx query)

          ;; 2️⃣ Fetch users in entitlement group
          query (-> (sql/select :egu.id :egu.type :u.firstname :u.lastname :u.email :u.searchable)
                  (sql/from [:entitlement_groups_users :egu])
                  (sql/join [:users :u] [:= :egu.user_id :u.id])
                  (sql/where [:= :egu.entitlement_group_id entitlement-group-id])
                  sql-format)
          users-groups (jdbc/execute! tx query)

          ;; 3️⃣ Fetch models linked to entitlement group
          query (-> (sql/select
                      [:m.id :model_id]
                      :m.name
                      :e.id
                      :e.entitlement_group_id
                      :e.quantity)
                  (sql/from [:entitlements :e])
                  (sql/join [:models :m] [:= :e.model_id :m.id])
                  (sql/where [:= :e.entitlement_group_id entitlement-group-id])
                  sql-format)
          models (jdbc/execute! tx query)
          model-ids (mapv :model_id models)

          ;; 4️⃣ Fetch item counts per model
          model-ids (to-uuid model-ids)
          models2 (select-entitlements-with-item-count tx pool-id model-ids entitlement-group-id)

          ;; 5️⃣ Join models + counts, compute availability
          models3 (->> (join-by :model_id models models2)
                    add-allocation-considered-count)

          ;; 6️⃣ Fetch linked groups
          query (-> (sql/select :egg.id :egg.group_id :g.name :g.searchable)
                  (sql/from [:entitlement_groups_groups :egg])
                  (sql/join [:groups :g] [:= :egg.group_id :g.id])
                  (sql/where [:= :egg.entitlement_group_id entitlement-group-id])
                  sql-format)
          groups (jdbc/execute! tx query)

          result {:entitlement-group entitlement-group
                  :users users-groups
                  :groups groups
                  :models models3}]

      (response result))

    (catch Exception e
      (error e "Error fetching entitlement group")
      (exception-handler request ERROR_GET e))))
