(ns leihs.inventory.server.resources.models.queries
  (:require
   [clojure.set]
   [clojure.string :refer [capitalize]]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.models.tree.descendents :refer [descendent-ids]]
   [taoensso.timbre :as timbre :refer [debug spy]]))

(defn item-query [query item-id]
  (-> query
      (cond-> item-id
        (sql/where [:= :i.id item-id]))))

(defn entitlements-query [query entitlement-id]
  (-> query
      (sql/select-distinct [:e.*])
      (sql/join [:entitlements :e] [:= :m.id :e.model_id])
      (cond-> entitlement-id
        (sql/where [:= :e.id entitlement-id]))))

(defn model-links-query [query model-links-id pool_id]
  (-> query
      (sql/select-distinct [:ml.*])
      (cond-> (nil? pool_id) (sql/join [:model_links :ml] [:= :m.id :ml.model_id]))
      (cond-> model-links-id
        (sql/where [:= :ml.id model-links-id]))))

(defn properties-query [query properties-id]
  (-> query
      (sql/select-distinct [:p.*])
      (sql/join [:properties :p] [:= :m.id :p.model_id])
      (cond-> properties-id
        (sql/where [:= :p.id properties-id]))))

(defn accessories-query
  ([query pool-id model-id accessories-id]
   (accessories-query query accessories-id "n/d"))
  ([query pool-id model-id accessories-id type]
   (-> query
       (sql/select-distinct [:a.*])
       (sql/join [:accessories :a] [:= :m.id :a.model_id])
       (sql/where [:= :models.id model-id])
       (cond-> accessories-id
         (sql/where [:= :a.id accessories-id])))))

(defn attachments-query
  ([query attachment-id]
   (attachments-query query attachment-id "n/d"))
  ([query attachment-id type]
   (-> query
       (sql/select-distinct :a.id :a.content :a.filename :a.item_id)
       (sql/join [:attachments :a] [:= :m.id :a.model_id])
       (cond-> attachment-id
         (sql/where [:= :a.id attachment-id])))))

(def base-pool-query
  (-> (sql/select :models.*)
      (sql/from :models)
      (sql/order-by [[:trim [:|| :models.product :models.version]] :asc])))

(defn base-inventory-query [pool-id]
  (-> (sql/select :inventory.*)
      (sql/from :inventory)
      (sql/where [:or
                  [:= :inventory.inventory_pool_id nil]
                  [:= :inventory.inventory_pool_id pool-id]])
      (sql/order-by :name)))

(defn filter-by-type [query type]
  (-> query
      (sql/where (case type
                   :package [:= :inventory.is_package true]
                   [:= :inventory.type (-> type name capitalize)]))))

(defn owner-or-responsible-cond [pool-id]
  [:or
   [:= :items.owner_id pool-id]
   [:= :items.inventory_pool_id pool-id]])

(defn owner-but-not-responsible-cond [pool-id pool2-id]
  [:and
   [:= :items.owner_id pool-id]
   [:= :items.inventory_pool_id pool2-id]])

(defn in-stock [query true-or-false]
  (-> query
      (sql/where [:= :items.parent_id nil])
      (sql/where
       (cond-> [:exists
                (-> (sql/select 1)
                    (sql/from :reservations)
                    (sql/where [:= :reservations.item_id :items.id])
                    (sql/where [:and
                                [:= :reservations.status ["signed"]]
                                [:= :reservations.returned_date nil]]))]
         true-or-false
         (vector :not)))))

(defn with-items [query pool-id
                  & {:keys [retired borrowable incomplete broken
                            inventory_pool_id owned
                            in_stock before_last_check]}]
  (sql/where
   query
   [:exists (-> (sql/select 1)
                (sql/from :items)
                (sql/where [:= :items.model_id :inventory.id])
                (#(cond
                    inventory_pool_id
                    (sql/where % (owner-but-not-responsible-cond pool-id inventory_pool_id))
                    owned
                    (sql/where % [:= :items.owner_id pool-id])
                    :else
                    (sql/where % (owner-or-responsible-cond pool-id))))
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
                  (sql/where [:= :items.is_incomplete incomplete])))]))

(defn without-items [query]
  (-> query
      (sql/where [:<> :inventory.type "Option"])
      (sql/where
       [:not [:exists (-> (sql/select 1)
                          (sql/from :items)
                          (sql/where [:= :items.model_id :inventory.id]))]])))

(defn with-search [query search]
  (sql/where query [:ilike
                    [:concat_ws ", "
                     :inventory.manufacturer :inventory.product :inventory.version]
                    (str "%" search "%")]))

(defn from-category [tx query category-id]
  (let [ids (-> (descendent-ids tx category-id)
                (conj category-id))]
    (-> query
        (sql/where
         [:exists (-> (sql/select 1)
                      (sql/from :model_links)
                      (sql/where [:= :model_links.model_id :inventory.id])
                      (sql/where [:in :model_links.model_group_id ids]))]))))
