(ns leihs.inventory.server.middlewares.authorize
  (:require
   [leihs.core.core :refer [detect]]
   [taoensso.timbre :as timbre :refer [debug spy]]))

(defn wrap-authorize-for-pool [handler]
  (fn [{{{pool-id :pool_id} :path} :parameters
        :keys [authenticated-entity]
        :as request}]
    (let [access-right (detect #(= (:inventory_pool_id %) pool-id)
                               (:access-rights authenticated-entity))
          role (:role access-right)]
      (if (contains? #{"lending_manager" "inventory_manager"} role)
        (handler (assoc-in request [:authenticated-entity :role] role))
        {:status 403 :body "Unauthorized due to insufficient access right role."}))))
