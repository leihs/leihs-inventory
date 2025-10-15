(ns leihs.inventory.server.resources.pool.items.shared
  (:require
   [clojure.set]
   [honey.sql :as sq :refer [format]]
   [honey.sql.helpers :as sql]
   [ring.middleware.accept]))

(defn in-stock [query true-or-false]
  (-> query
      (sql/where [:= :i.parent_id nil])
      (sql/where
       [(if true-or-false :not-exists :exists)
        (-> (sql/select 1)
            (sql/from :reservations)
            (sql/where [:= :reservations.item_id :i.id])
            (sql/where [:and
                        [:= :reservations.status ["signed"]]
                        [:= :reservations.returned_date nil]]))])))

(defn owner-or-responsible-cond [pool-id]
  [:or
   [:= :i.owner_id pool-id]
   [:= :i.inventory_pool_id pool-id]])

(defn owner-and-responsible-cond [pool-id inventory-pool-id]
  [:and
   [:= :i.owner_id pool-id]
   [:= :i.inventory_pool_id inventory-pool-id]])

(defn not-owner-and-responsible-cond [pool-id inventory-pool-id]
  [:and
   [:not= :i.owner_id pool-id]
   [:= :i.inventory_pool_id inventory-pool-id]])

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
          (sql/where % [:= :i.owner_id pool-id])

          (false? owned)
          (sql/where % [:not= :i.owner_id pool-id])

          :else %))
      (cond-> (boolean? in_stock) (in-stock in_stock))
      (cond-> before_last_check
        (sql/where [:<= :i.last_check before_last_check]))
      (cond-> (boolean? retired)
        (sql/where [(if retired :<> :=) :i.retired nil]))
      (cond-> (boolean? borrowable)
        (sql/where [:= :i.is_borrowable borrowable]))
      (cond-> (boolean? broken)
        (sql/where [:= :i.is_broken broken]))
      (cond-> (boolean? incomplete)
        (sql/where [:= :i.is_incomplete incomplete]))))
