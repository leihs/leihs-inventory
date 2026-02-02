(ns leihs.inventory.server.resources.pool.entitlement-groups.common
  (:require
   [clojure.set :as set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [next.jdbc :as jdbc]))

(defn fetch-entitlements [tx entitlement-group-id]
  (let [query (-> (sql/select :e.id :e.model_id :e.entitlement_group_id :e.quantity)
                  (sql/from [:entitlements :e])
                  (sql/where [:= :e.entitlement_group_id (to-uuid entitlement-group-id)])
                  sql-format)
        db-entitlements (jdbc/execute! tx query)
        db-entitlement-ids (map :id db-entitlements)
        db-model-ids (map :model_id db-entitlements)]
    {:db-entitlements db-entitlements
     :db-entitlement-ids db-entitlement-ids
     :db-model-ids db-model-ids}))

(defn delete-entitlements [tx model-ids entitlement-group-id]
  (when (seq model-ids)
    (jdbc/execute! tx (-> (sql/delete-from :entitlements)
                          (sql/where [:and [:in :model_id model-ids] [:= :entitlement_group_id entitlement-group-id]])
                          (sql/returning :*)
                          sql-format))))

(defn create-entitlements [tx entitlements]
  (when (seq entitlements)
    (jdbc/execute! tx (-> (sql/insert-into :entitlements)
                          (sql/values entitlements)
                          (sql/returning :*)
                          sql-format))))

(defn update-entitlements [tx entitlements entitlement-group-id]
  (when (seq entitlements)
    (vec
     (mapcat
      (fn [{:keys [model_id quantity]}]
        (jdbc/execute!
         tx
         (-> (sql/update :entitlements)
             (sql/set {:quantity quantity})
             (sql/where [:and
                         [:= :model_id model_id]
                         [:= :entitlement_group_id entitlement-group-id]])
             (sql/returning :*)
             sql-format)))
      entitlements))))

(defn link-users-to-entitlement-group [tx users entitlement-group-id]
  (let [entitlement-group-id (to-uuid entitlement-group-id)
        existing (-> (sql/select :id :user_id)
                     (sql/from :entitlement_groups_direct_users)
                     (sql/where [:= :entitlement_group_id entitlement-group-id])
                     sql-format
                     (->> (jdbc/execute! tx)))
        existing-map (into {} (map (juxt :user_id :id) existing))
        existing-ids (set (keys existing-map))
        incoming-ids (set users)
        ids-to-delete (clojure.set/difference existing-ids incoming-ids)
        ids-to-create (clojure.set/difference incoming-ids existing-ids)

        deleted-rows (when (seq ids-to-delete)
                       (jdbc/execute! tx
                                      (-> (sql/delete-from :entitlement_groups_direct_users)
                                          (sql/where [:and
                                                      [:= :entitlement_group_id entitlement-group-id]
                                                      [:in :user_id (vec ids-to-delete)]])
                                          (sql/returning :*)
                                          sql-format)))

        rows-to-create (map (fn [uid]
                              {:user_id uid
                               :entitlement_group_id entitlement-group-id})
                            ids-to-create)

        created-rows (when (seq rows-to-create)
                       (jdbc/execute! tx
                                      (-> (sql/insert-into :entitlement_groups_direct_users)
                                          (sql/values rows-to-create)
                                          (sql/returning :*)
                                          sql-format)))]
    {:deleted deleted-rows
     :created created-rows}))

(defn link-groups-to-entitlement-group [tx groups entitlement-group-id]
  (let [entitlement-group-id (to-uuid entitlement-group-id)
        now (java.sql.Timestamp/from (java.time.Instant/now))
        existing (-> (sql/select :id :group_id)
                     (sql/from :entitlement_groups_groups)
                     (sql/where [:= :entitlement_group_id entitlement-group-id])
                     sql-format
                     (->> (jdbc/execute! tx)))

        existing-map (into {} (map (juxt :group_id :id) existing))
        existing-ids (set (keys existing-map))
        incoming-ids (set groups)
        ids-to-delete (clojure.set/difference existing-ids incoming-ids)
        ids-to-create (clojure.set/difference incoming-ids existing-ids)

        deleted-rows (when (seq ids-to-delete)
                       (jdbc/execute! tx
                                      (-> (sql/delete-from :entitlement_groups_groups)
                                          (sql/where [:and
                                                      [:= :entitlement_group_id entitlement-group-id]
                                                      [:in :group_id (vec ids-to-delete)]])
                                          (sql/returning :*)
                                          sql-format)))

        rows-to-create (map (fn [gid]
                              {:group_id gid
                               :entitlement_group_id entitlement-group-id
                               :created_at now
                               :updated_at now})
                            ids-to-create)

        created-rows (when (seq rows-to-create)
                       (jdbc/execute! tx
                                      (-> (sql/insert-into :entitlement_groups_groups)
                                          (sql/values rows-to-create)
                                          (sql/returning :*)
                                          sql-format)))]

    {:deleted deleted-rows
     :created created-rows}))
