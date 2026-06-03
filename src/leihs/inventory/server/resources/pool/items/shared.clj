(ns leihs.inventory.server.resources.pool.items.shared
  (:require
   [clojure.set]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.list.search :as list-search]
   [ring.middleware.accept]
   [taoensso.timbre :as timbre :refer [debug spy]]))

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

(defn item-query-params [query pool-id &
                         {:keys [inventory_pool_id
                                 owned in_stock before_last_check
                                 retired borrowable broken incomplete]}]
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

(defn filtered-items-subquery
  "Correlated subquery of item ids for list export — same filters/search as GET /items/."
  [pool-id {:keys [search inventory_pool_id owned in_stock before_last_check
                   retired borrowable broken incomplete]}]
  (-> (sql/select :items.id)
      (sql/from :items)
      (sql/where [:= :items.model_id :inventory.id])
      (sql/where (owner-or-responsible-cond pool-id))
      (item-query-params pool-id
                         :inventory_pool_id inventory_pool_id
                         :owned owned
                         :in_stock in_stock
                         :before_last_check before_last_check
                         :retired retired
                         :borrowable borrowable
                         :broken broken
                         :incomplete incomplete)
      (cond-> search
        (list-search/with-search search :inventory))))

(defn items-join-conditions
  [pool-id with-items? item-opts]
  (if (and with-items? item-opts)
    [:and [:= :inventory.id :items.model_id]
     [:in :items.id (filtered-items-subquery pool-id item-opts)]]
    [:and [:= :inventory.id :items.model_id]
     (owner-or-responsible-cond pool-id)]))
