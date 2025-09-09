(ns leihs.inventory.server.resources.pool.models.queries
  (:require
   [clojure.set]
   [hugsql.core :as hugsql]))

(hugsql/def-sqlvec-fns "sql/descendent_ids.sql")

(declare descendent-ids-sqlvec)

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
