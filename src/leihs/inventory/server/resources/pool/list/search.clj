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

(defn matches-children-expr [search]
  [:exists
   (-> (sql/select 1)
       (sql/from [:items :parent_items])
       (sql/join [:items :child_items] [:= :child_items.parent_id :parent_items.id])
       (sql/join [:models :child_models] [:= :child_models.id :child_items.model_id])
       (sql/where [:= :parent_items.model_id :inventory.id])
       (sql/where
        [:or
         (matches-model-columns-expr search :child_models)
         (matches-item-columns-expr search :child_items)]))])

(defn with-search-for-select-count [query search]
  (sql/where
   query
   [:or
    (matches-model-columns-expr search :inventory)
    (matches-item-columns-expr search :items)
    (matches-children-expr search)]))

(defn with-search [query search]
  (sql/where
   query
   [:or
    (matches-model-columns-expr search :inventory)
    [:exists
     (-> (sql/select 1)
         (sql/from :items)
         (sql/where [:= :items.model_id :inventory.id])
         (sql/where (matches-item-columns-expr search :items)))]
    (matches-children-expr search)]))
