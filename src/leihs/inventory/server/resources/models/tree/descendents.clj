(ns leihs.inventory.server.resources.models.tree.descendents
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.models.tree.shared :as shared]
   [next.jdbc :as jdbc]))

(defn children [tx category
                & {:keys [with-metadata exclude] :or {with-metadata false}}]
  (-> (sql/select [:model_group_links.child_id :category_id]
                  :model_groups.name)
      (cond-> with-metadata
        (shared/sql-add-metadata :label :model_group_links.label
                                 :exclude exclude))
      (sql/from :model_group_links)
      (sql/join :model_groups [:= :model_group_links.child_id :model_groups.id])
      (sql/where [:= :model_group_links.parent_id (:category_id category)])
      sql-format
      (->> (jdbc/execute! tx))))

(defn descendents [tx initial-category
                   & {:keys [with-metadata exclude] :or {with-metadata false}}]
  (letfn [(descendents-h [category visited-ids]
            (if (contains? visited-ids (:category_id category))
              category
              (let [visited-ids* (conj visited-ids (:category_id category))
                    children-with-descendents
                    (map #(descendents-h % visited-ids*)
                         (children tx category
                                   :with-metadata with-metadata
                                   :exclude exclude))]
                (assoc category :children children-with-descendents))))]
    (descendents-h initial-category #{})))