(ns leihs.inventory.server.resources.pool.entitlement-groups.entitlement-group.query
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.entitlement-groups.common :refer [fetch-entitlements]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.request-utils :refer [path-params]]
   [next.jdbc :as jdbc]))

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

(defn fetch-entitlement-group [tx request]
  (let [pool-id (-> request path-params :pool_id)
        entitlement-group-id (-> request path-params :entitlement_group_id)

        query (-> (sql/select :g.id :g.name :g.is_verification_required)
                  (sql/from [:entitlement_groups :g])
                  (sql/join [:inventory_pools :ip] [:= :g.inventory_pool_id :ip.id])
                  (sql/where [:and
                              [:= :g.inventory_pool_id pool-id]
                              [:= :g.id entitlement-group-id]])
                  (sql/order-by :g.name)
                  sql-format)
        entitlement-group (jdbc/execute-one! tx query)] entitlement-group))

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

(defn fetch-users-of-entitlement-group [tx entitlement-group-id]
  (let [query (-> (sql/select :egu.id :egu.type :egu.user_id :u.firstname :u.lastname :u.email :u.searchable)
                  (sql/from [:entitlement_groups_users :egu])
                  (sql/join [:users :u] [:= :egu.user_id :u.id])
                  (sql/where [:= :egu.entitlement_group_id entitlement-group-id])
                  sql-format)
        users-groups (jdbc/execute! tx query)]
    users-groups))

(defn fetch-models-of-entitlement-group [tx request]
  (let [pool-id (-> request path-params :pool_id)
        entitlement-group-id (-> request path-params :entitlement_group_id)
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
        model-ids (mapv :model_id models)]
    (if (seq model-ids)
      (let [model-ids (to-uuid model-ids)
            models2 (select-entitlements-with-item-count tx pool-id model-ids entitlement-group-id)
            models3 (->> (join-by :model_id models models2)
                         add-allocation-considered-count)] models3)
      [])))

(defn fetch-groups-of-entitlement-group [tx entitlement-group-id]
  (let [query (-> (sql/select :egg.id :egg.group_id :g.name :g.searchable)
                  (sql/from [:entitlement_groups_groups :egg])
                  (sql/join [:groups :g] [:= :egg.group_id :g.id])
                  (sql/where [:= :egg.entitlement_group_id entitlement-group-id])
                  sql-format)
        groups (jdbc/execute! tx query)]
    groups))

(defn update-entitlement-groups [tx entitlement-group entitlement-group-id]
  (let [eg-data (select-keys entitlement-group [:name :is_verification_required])
        query (-> (sql/update :entitlement_groups)
                  (sql/set eg-data)
                  (sql/where [:= :id (to-uuid entitlement-group-id)])
                  (sql/returning :*)
                  sql-format)
        entitlement-group (jdbc/execute-one! tx query)]
    entitlement-group))

(defn analyze-and-prepare-data [tx models entitlement_group_id]
  (let [{:keys [db-entitlement-ids]} (fetch-entitlements tx entitlement_group_id)
        entitlements-to-update (filterv :id models)
        entitlements-to-create (vec (remove :id models))
        entitlements-to-create (mapv (fn [item]
                                       (assoc item :entitlement_group_id entitlement_group_id))
                                     entitlements-to-create)
        entitlement-ids (set (mapv :id models))
        entitlement-ids-to-delete (remove entitlement-ids db-entitlement-ids)]

    {:entitlements-to-update entitlements-to-update
     :entitlements-to-create entitlements-to-create
     :entitlement-ids-to-delete entitlement-ids-to-delete}))
