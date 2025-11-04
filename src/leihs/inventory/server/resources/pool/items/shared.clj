(ns leihs.inventory.server.resources.pool.items.shared
  (:require
   [clojure.set]
   [honey.sql.helpers :as sql]
   [ring.middleware.accept]))

(defn in-stock [query true-or-false]
  (-> query
      (sql/where [:= :items.parent_id nil])
      (sql/where
       [(if true-or-false :not-exists :exists)
        (-> (sql/select 1)
            (sql/from :reservations)
            (sql/where [:= :reservations.item_id :items.id])
            (sql/where [:and
                        [:= :reservations.status ["signed"]]
                        [:= :reservations.returned_date nil]]))])))

(defn owner-or-responsible-cond [pool-id]
  [:or
   [:= :items.owner_id pool-id]
   [:= :items.inventory_pool_id pool-id]])

(defn owner-and-responsible-cond [pool-id inventory-pool-id]
  [:and
   [:= :items.owner_id pool-id]
   [:= :items.inventory_pool_id inventory-pool-id]])

(defn not-owner-and-responsible-cond [pool-id inventory-pool-id]
  [:and
   [:not= :items.owner_id pool-id]
   [:= :items.inventory_pool_id inventory-pool-id]])

(defn item-query-params [query pool-id inventory_pool_id
                         owned in_stock before_last_check
                         retired borrowable broken incomplete]
  (-> query
      (#(cond
          (and inventory_pool_id (true? owned))
          (sql/where % (owner-and-responsible-cond pool-id inventory_pool_id))

          (and inventory_pool_id (false? owned))
          (sql/where % (not-owner-and-responsible-cond pool-id inventory_pool_id))

          inventory_pool_id
          (sql/where % (owner-or-responsible-cond inventory_pool_id))

          (true? owned)
          (sql/where % [:= :items.owner_id pool-id])

          (false? owned)
          (sql/where % [:not= :items.owner_id pool-id])

          :else %))
      (cond-> (boolean? in_stock) (in-stock in_stock))
      (cond-> before_last_check
        (sql/where [:<= :items.last_check before_last_check]))
      (cond-> (boolean? retired)
        (sql/where [(if retired :<> :=) :items.retired nil]))
      (cond-> (boolean? borrowable)
        (sql/where [:= :items.is_borrowable borrowable]))
      (cond-> (boolean? broken)
        (sql/where [:= :items.is_broken broken]))
      (cond-> (boolean? incomplete)
        (sql/where [:= :items.is_incomplete incomplete]))))
