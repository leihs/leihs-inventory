(ns leihs.inventory.server.middlewares.authorize.main
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.core :refer [detect]]
   [next.jdbc :as jdbc]))

(def AUTHORIZED-ROLES #{"lending_manager" "inventory_manager"})

(def READONLY-ROLES
  "Extra pool roles beyond AUTHORIZED-ROLES, for read-only API slices."
  #{"group_manager"})

(defn authorized-role-for-pool [request pool-id]
  (let [access-right
        (detect #(= (:inventory_pool_id %) pool-id)
                (get-in request [:authenticated-entity :access-rights]))]
    (:role access-right)))

(defn pool-active? [tx pool-id]
  (-> (sql/select 1)
      (sql/from :inventory_pools)
      (sql/where [:= :id pool-id])
      (sql/where [:= :is_active true])
      sql-format
      (->> (jdbc/execute-one! tx))
      boolean))
