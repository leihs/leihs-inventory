(ns leihs.inventory.server.resources.pool.list.queries
  (:require
   [clojure.set]
   [clojure.string :refer [capitalize]]
   [honey.sql.helpers :as sql]
   [hugsql.core :as hugsql]
   [leihs.core.core :refer [presence]]
   [leihs.inventory.server.resources.pool.items.shared :as items-shared]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]))

(defn base-inventory-query [pool-id]
  (-> (sql/select :inventory.*
                  [(-> (sql/select :%count.*) ; [[:count :*]]
                       (sql/from :items)
                       (sql/where [:and
                                   [:= :items.inventory_pool_id pool-id]
                                   [:= :items.model_id :inventory.id]
                                   [:= :items.is_borrowable true]]))
                   :rentable]

                  [(-> (sql/select :%count.*) ; [[:count :*]]
                       (sql/from :items)
                       (sql/where [:and
                                   [:= :items.inventory_pool_id pool-id]
                                   [:= :items.parent_id nil]
                                   [:= :items.is_borrowable true]
                                   [:= :items.model_id :inventory.id]
                                   [:not [:exists
                                          (-> (sql/select 1)
                                              (sql/from :reservations)
                                              (sql/where [:and
                                                          [:= :reservations.returned_date nil]
                                                          [:= :items.id :reservations.item_id]]))]]]))
                   :in_stock])

      (sql/from :inventory)
      (sql/where [:or
                  [:= :inventory.inventory_pool_id nil]
                  [:= :inventory.inventory_pool_id pool-id]])

      (sql/order-by [[:regexp_replace :inventory.name "^\\s+|\\s+$" ""]])))

(defn filter-by-type [query type]
  (-> query
      (sql/where [:= :inventory.type (-> type name capitalize)])))

(defn select-items-count [query pool-id
                          & {:keys [retired borrowable incomplete broken
                                    inventory_pool_id owned search
                                    in_stock before_last_check]}]
  (-> query
      (sql/select
       [(-> (sql/select [[:case
                          [:= :inventory.type "Option"] nil
                          :else
                          :%count.*]])
            (sql/from :items)
            (sql/where [:= :items.model_id :inventory.id])
            (sql/where (items-shared/owner-or-responsible-cond pool-id))
            (items-shared/item-query-params pool-id
                                            :inventory_pool_id inventory_pool_id
                                            :owned owned
                                            :in_stock in_stock
                                            :before_last_check before_last_check
                                            :retired retired
                                            :borrowable borrowable
                                            :broken broken
                                            :incomplete incomplete)
            (cond-> (presence search) (with-search-for-count search :inventory)))
        :items])))

(defn all-items [query pool-id
                 & {:keys [retired borrowable incomplete broken
                           inventory_pool_id owned search
                           in_stock before_last_check]}]
  (-> query
      (select-items-count pool-id
                          :retired retired
                          :borrowable borrowable
                          :incomplete incomplete
                          :broken broken
                          :inventory_pool_id inventory_pool_id
                          :owned owned
                          :in_stock in_stock
                          :before_last_check before_last_check
                          :search search)))

(defn with-items [query pool-id
                  & {:keys [retired borrowable incomplete broken
                            inventory_pool_id owned search
                            in_stock before_last_check]}]
  (-> query
      (select-items-count pool-id
                          :retired retired
                          :borrowable borrowable
                          :incomplete incomplete
                          :broken broken
                          :inventory_pool_id inventory_pool_id
                          :owned owned
                          :in_stock in_stock
                          :before_last_check before_last_check
                          :search search)
      (sql/where
       [:exists (-> (sql/select 1)
                    (sql/from :items)
                    (sql/where [:= :items.model_id :inventory.id])
                    (sql/where (items-shared/owner-or-responsible-cond pool-id))
                    (items-shared/item-query-params pool-id
                                                    :inventory_pool_id inventory_pool_id
                                                    :owned owned
                                                    :in_stock in_stock
                                                    :before_last_check before_last_check
                                                    :retired retired
                                                    :borrowable borrowable
                                                    :broken broken
                                                    :incomplete incomplete))])))

(defn without-items [query pool-id]
  (-> query
      (sql/select [0 :items])
      (sql/where [:<> :inventory.type "Option"])
      (sql/where
       [:not [:exists (-> (sql/select 1)
                          (sql/from :items)
                          (sql/where [:= :items.model_id :inventory.id])
                          (sql/where (items-shared/owner-or-responsible-cond pool-id)))]])))

(hugsql/def-sqlvec-fns "sql/descendent_ids.sql")

(defn descendent-ids [tx category-id]
  (-> {:category-id category-id}
      descendent-ids-sqlvec
      (->> (jdbc-query tx))
      (->> (map :id))))

(defn from-category [tx query category-id]
  (let [ids (-> (descendent-ids tx category-id)
                (conj category-id))]
    (-> query
        (sql/where
         [:exists (-> (sql/select 1)
                      (sql/from :model_links)
                      (sql/where [:= :model_links.model_id :inventory.id])
                      (sql/where [:in :model_links.model_group_id ids]))]))))
