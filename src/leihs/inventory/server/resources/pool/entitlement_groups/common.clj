(ns leihs.inventory.server.resources.pool.entitlement-groups.common
  (:require
   [clojure.set :as set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [next.jdbc :as jdbc]))

(defn unique-ids
  "Return items unique to either vec-a or vec-b (symmetric difference)."
  [vec-a vec-b]
  (vec
   (set/difference
    (set (concat vec-a vec-b))
    (set/intersection (set vec-a) (set vec-b)))))

(defn fetch-entitlements [tx entitlement-group-id]
  (let [query (-> (sql/select :e.id :e.model_id :e.entitlement_group_id :e.quantity)
                  (sql/from [:entitlements :e])
                  (sql/where [:= :e.entitlement_group_id (to-uuid entitlement-group-id)])
                  sql-format)
        db-entitlements (jdbc/execute! tx query)
        db-entitlement-ids (mapv :id db-entitlements)]
    {:db-entitlements db-entitlements
     :db-entitlement-ids db-entitlement-ids}))

(defn delete-entitlements [tx entitlement-ids]
  (if (seq entitlement-ids)
    (jdbc/execute! tx (-> (sql/delete-from :entitlements)
                          (sql/where [:in :id entitlement-ids])
                          (sql/returning :*)
                          sql-format))
    []))

(defn create-entitlements [tx entitlements]
  (if (seq entitlements)
    (jdbc/execute! tx (-> (sql/insert-into :entitlements)
                          (sql/values entitlements)
                          (sql/returning :*)
                          sql-format))
    []))

(defn update-entitlements [tx entitlements]
  (if (seq entitlements)
    (->> entitlements
         (mapv (fn [entitlement]
                 (jdbc/execute! tx (-> (sql/update :entitlements)
                                       (sql/set (select-keys entitlement [:quantity]))
                                       (sql/where [:= :id (:id entitlement)])
                                       (sql/returning :*)
                                       sql-format))))
         (apply concat)
         vec)
    []))

(defn link-users-to-entitlement-group [tx users entitlement-group-id]
  (let [existing-users (-> (sql/select :id :user_id)
                           (sql/from :entitlement_groups_direct_users)
                           (sql/where [:= :entitlement_group_id (to-uuid entitlement-group-id)])
                           sql-format
                           (->> (jdbc/execute! tx)
                                (mapv :id)))

        ids-to-update (mapv :id (filter :id users))
        ids-to-delete (unique-ids ids-to-update existing-users)
        deleted (if (seq ids-to-delete)
                  (jdbc/execute! tx (-> (sql/delete-from :entitlement_groups_direct_users)
                                        (sql/where [:in :id ids-to-delete])
                                        (sql/returning :*)
                                        sql-format))
                  [])

        users-to-create (remove :id users)
        users-to-create (mapv #(assoc % :entitlement_group_id entitlement-group-id) users-to-create)
        created (if (seq users-to-create)
                  (jdbc/execute! tx (-> (sql/insert-into :entitlement_groups_direct_users)
                                        (sql/values users-to-create)
                                        (sql/returning :*)
                                        sql-format))
                  [])]
    {:deleted deleted
     :created created}))

(defn link-groups-to-entitlement-group [tx groups entitlement-group-id]
  (let [now (java.sql.Timestamp/from (java.time.Instant/now))
        db-groups (-> (sql/select :id)
                      (sql/from :entitlement_groups_groups)
                      (sql/where [:= :entitlement_group_id (to-uuid entitlement-group-id)])
                      sql-format
                      (->> (jdbc/execute! tx)
                           (mapv :id)))

        ids-to-update (mapv :id (filter :id groups))
        ids-to-delete (unique-ids ids-to-update db-groups)

        groups-to-create (remove :id groups)
        groups-to-create (mapv #(assoc % :created_at now
                                       :updated_at now
                                       :entitlement_group_id entitlement-group-id)
                               groups-to-create)

        deleted-groups (if (seq ids-to-delete)
                         (jdbc/execute! tx (-> (sql/delete-from :entitlement_groups_groups)
                                               (sql/where [:in :id ids-to-delete])
                                               (sql/returning :*)
                                               sql-format))
                         [])
        created-groups (if (seq groups-to-create)
                         (jdbc/execute! tx (-> (sql/insert-into :entitlement_groups_groups)
                                               (sql/values groups-to-create)
                                               (sql/returning :*)
                                               sql-format))
                         [])]
    {:deleted deleted-groups
     :created created-groups}))
