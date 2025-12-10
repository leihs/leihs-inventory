(ns leihs.inventory.server.resources.pool.list.search
  (:require
   [clojure.set]
   [honey.sql.helpers :as sql]))

(defn matches-model-columns-expr [search table]
  [:ilike
   [:concat_ws " "
    (keyword (name table) "manufacturer")
    (keyword (name table) "product")
    (keyword (name table) "version")]
   (str "%" search "%")])

(defn matches-item-columns-expr [search table]
  [:ilike
   [:concat_ws " "
    (keyword (name table) "inventory_code")
    (keyword (name table) "serial_number")
    (keyword (name table) "invoice_number")
    (keyword (name table) "note")
    (keyword (name table) "name")
    (keyword (name table) "user_name")
    (keyword (name table) "properties")]
   (str "%" search "%")])

(defn matches-children-for-count-expr [search ref-table]
  [:exists
   (-> (sql/select 1)
       (sql/from [:items :child_items])
       (sql/join [:items :parent_items]
                 [:= :parent_items.id :child_items.parent_id])
       (sql/join [:models :child_models]
                 [:= :child_models.id :child_items.model_id])
       (sql/where [:= :child_items.parent_id :items.id])
       (sql/where [:= :parent_items.model_id (keyword (name ref-table) "id")])
       (sql/where
        [:or
         (matches-model-columns-expr search :child_models)
         (matches-item-columns-expr search :child_items)]))])

(defn matches-children-expr [search ref-table]
  [:exists
   (-> (sql/select 1)
       (sql/from [:items :parent_items])
       (sql/join [:items :child_items]
                 [:= :child_items.parent_id :parent_items.id])
       (sql/join [:models :child_models]
                 [:= :child_models.id :child_items.model_id])
       (sql/where [:= :parent_items.model_id (keyword (name ref-table) "id")])
       (sql/where
        [:or
         (matches-model-columns-expr search :child_models)
         (matches-item-columns-expr search :child_items)]))])

(defn with-search [query search ref-table]
  (sql/where
   query
   [:or
    (matches-model-columns-expr search ref-table)
    (matches-item-columns-expr search :items)
    (matches-children-expr search ref-table)]))

(defn with-search-for-count [query search ref-table]
  (sql/where
   query
   [:or
    (matches-model-columns-expr search ref-table)
    (matches-item-columns-expr search :items)
    (matches-children-for-count-expr search ref-table)]))

(defn with-search-inventory [query search]
  (sql/where
   query
   [:or
    (matches-model-columns-expr search :inventory)
    [:exists
     (-> (sql/select 1)
         (sql/from :items)
         (sql/where [:= :items.model_id :inventory.id])
         (sql/where (matches-item-columns-expr search :items)))]
    (matches-children-expr search :inventory)]))
