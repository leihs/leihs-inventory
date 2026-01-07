(ns leihs.inventory.server.resources.pool.entitlement-groups.entitlement-group.query
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.entitlement-groups.common :refer [fetch-entitlements]]
   [leihs.inventory.server.resources.pool.models.common :refer [fetch-thumbnails-for-ids
                                                                model->enrich-with-image-attr]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.request-utils :refer [path-params]]
   [next.jdbc :as jdbc]))

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

(defn select-allocations [tx inventory-pool-id model-ids exclude-group-id]
  (let [keyname (if exclude-group-id
                  :entitled_in_other_groups
                  :entitled_in_groups)
        query (-> (sql/select
                   [:e.model_id :id]
                   [[:cast [:coalesce [:sum :e.quantity] 0] :integer] keyname])
                  (sql/from [:entitlements :e])
                  (sql/join [:entitlement_groups :eg] [:= :eg.id :e.entitlement_group_id])
                  (sql/where [:and
                              [:= :eg.inventory_pool_id inventory-pool-id]
                              [:in :e.model_id model-ids]])
                  (cond-> exclude-group-id
                    (sql/where [:!= :e.entitlement_group_id exclude-group-id]))
                  (sql/group-by :e.model_id)
                  sql-format)]
    (jdbc/execute! tx query)))

(defn- select-available
  "Selects item counts for models in the inventory pool."
  [tx inventory-pool-id model-ids]
  (let [query
        (-> (sql/select
             [:items.model_id :id]
             [[[:count :items.id]] :available])
            (sql/from :items)
            (sql/where [:and
                        [:in :items.model_id model-ids]
                        [:is :items.retired nil]
                        [:= :items.is_borrowable true]
                        [:is :items.parent_id nil]
                        [:= :items.inventory_pool_id inventory-pool-id]])
            (sql/group-by :items.model_id)
            sql-format)]
    (jdbc/execute! tx query)))

(defn- merge-results
  "Merges allocations and items count results.
   Ensures all model-ids have entries with 0 defaults for missing values."
  [model-ids allocations availables]
  (let [allocations-by-id (into {} (map (fn [row] [(:id row) row]) allocations))
        items-by-id (into {} (map (fn [row] [(:id row) row]) availables))]
    (mapv (fn [model-id]
            {:id model-id
             :entitled_in_other_groups
             (get-in allocations-by-id [model-id :entitled_in_other_groups] 0)
             :available
             (get-in items-by-id [model-id :available] 0)})
          model-ids)))

(defn select-entitlements-with-item-count [tx inventory-pool-id model-ids exclude-group-id]
  (let [allocations (select-allocations tx inventory-pool-id model-ids exclude-group-id)
        availables (select-available tx inventory-pool-id model-ids)]
    (merge-results model-ids allocations availables)))

(defn- ->long [v]
  (cond
    (nil? v) 0
    (number? v) (long v)
    (string? v) (try (Long/parseLong v)
                     (catch Exception _ 0))
    :else 0))

(defn add-allocation-considered-count [entitlements]
  (mapv (fn [e]
          (let [available-raw (:available e)
                quantity-raw (:quantity e)
                available (->long available-raw)
                quantity (->long quantity-raw)
                e (assoc e :available available
                         :is_quantity_ok (<= quantity available))
                result (dissoc e :entitlement_group_id)]
            result))
        entitlements))

(defn fetch-users-of-entitlement-group [tx entitlement-group-id]
  (let [query (-> (sql/select :u.id :u.firstname :u.lastname :u.email :u.searchable :u.account_enabled :egu.type :u.img32_url :u.img256_url)
                  (sql/from [:entitlement_groups_users :egu])
                  (sql/join [:users :u] [:= :egu.user_id :u.id])
                  (sql/where [:= :egu.entitlement_group_id entitlement-group-id])
                  (sql/order-by :u.lastname :u.firstname :u.id)
                  sql-format)
        users-groups (jdbc/execute! tx query)]
    users-groups))

(defn fetch-models-of-entitlement-group
  ([tx request]
   (let [pool-id (-> request path-params :pool_id)
         entitlement-group-id (-> request path-params :entitlement_group_id)]
     (fetch-models-of-entitlement-group tx pool-id entitlement-group-id)))

  ([tx pool-id entitlement-group-id]
   (let [query (-> (sql/select
                    :m.id
                    :m.product
                    :m.name
                    :m.version
                    [:e.id :entitlement_id]
                    :e.entitlement_group_id
                    :e.quantity)
                   (sql/from [:entitlements :e])
                   (sql/join [:models :m] [:= :e.model_id :m.id])
                   (sql/where [:= :e.entitlement_group_id entitlement-group-id])
                   (sql/order-by :m.product)
                   sql-format)
         models (jdbc/execute! tx query)
         model-ids (mapv :id models)]

     (if (seq model-ids)
       (let [models-with-images (->> models
                                     (fetch-thumbnails-for-ids tx)
                                     (map (model->enrich-with-image-attr pool-id)))
             model-ids (to-uuid model-ids)
             allocation-data (select-entitlements-with-item-count tx pool-id model-ids entitlement-group-id)
             allocation-map (->> allocation-data
                                 (map (juxt :id identity))
                                 (into {}))
             models-with-allocation (map (fn [model]
                                           (let [allocation (get allocation-map (:id model))]
                                             (merge model
                                                    {:entitled_in_other_groups (or (:entitled_in_other_groups allocation) 0)
                                                     :available (or (:available allocation) 0)})))
                                         models-with-images)]
         (add-allocation-considered-count models-with-allocation))
       []))))

(defn enrich-with-is-quantity-ok [tx pool-id entitlement-group-ids]
  (let [res (mapv (fn [eg-id]
                    (let [models (fetch-models-of-entitlement-group tx pool-id eg-id)
                          all-ok? (every? :is_quantity_ok models)]
                      {:id eg-id :is_quantity_ok all-ok?}))
                  entitlement-group-ids)]
    res))

(defn fetch-groups-of-entitlement-group [tx entitlement-group-id]
  (let [query (-> (sql/select :g.id :g.name :g.searchable)
                  (sql/from [:entitlement_groups_groups :egg])
                  (sql/join [:groups :g] [:= :egg.group_id :g.id])
                  (sql/where [:= :egg.entitlement_group_id entitlement-group-id])
                  (sql/order-by :g.name :g.id)
                  sql-format)
        groups (jdbc/execute! tx query)]
    groups))

(defn update-entitlement-group [tx entitlement-group entitlement-group-id]
  (let [eg-data (select-keys entitlement-group [:name :is_verification_required])
        query (-> (sql/update :entitlement_groups)
                  (sql/set eg-data)
                  (sql/where [:= :id (to-uuid entitlement-group-id)])
                  (sql/returning :*)
                  sql-format)
        entitlement-group (jdbc/execute-one! tx query)]
    entitlement-group))

(defn rename-key [m old new]
  (let [old-k (keyword old)
        old-s (name old)
        v (or (get m old-k)
              (get m old-s))]
    (cond-> m
      v (-> (assoc new v)
            (dissoc old-k old-s)))))

(defn analyze-and-prepare-data [tx models entitlement-group-id]
  (let [{:keys [db-model-ids]} (fetch-entitlements tx entitlement-group-id)
        db-model-id-set (set db-model-ids)
        incoming-model-ids (set (keep :id models))

        model-ids-to-delete (vec (remove incoming-model-ids db-model-ids))
        entitlements-to-update (mapv #(rename-key % :id :model_id)
                                     (filterv #(contains? db-model-id-set (:id %)) models))
        entitlements-to-create (mapv #(-> %
                                          (rename-key :id :model_id)
                                          (assoc :entitlement_group_id entitlement-group-id))
                                     (filterv #(not (contains? db-model-id-set (:id %))) models))]

    {:entitlements-to-update entitlements-to-update
     :entitlements-to-create entitlements-to-create
     :entitlement-ids-to-delete model-ids-to-delete}))
