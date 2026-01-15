(ns leihs.inventory.server.middlewares.authorize.main
  (:require
   [leihs.core.core :refer [detect]]))

(def AUTHORIZED-ROLES #{"lending_manager" "inventory_manager"})

(defn authorized-role-for-pool [request pool-id]
  (let [access-right
        (detect #(= (:inventory_pool_id %) pool-id)
                (get-in request [:authenticated-entity :access-rights]))]
    (:role access-right)))
