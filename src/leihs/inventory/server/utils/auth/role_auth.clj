(ns leihs.inventory.server.utils.auth.role-auth
  (:require
   [clojure.set]
   [ring.util.response :refer [response status]]
   [taoensso.timbre :refer [debug error spy]]))

(defn validate-request
  "Validates the user's access based on roles, scope, and optionally a pool ID."
  [auth-entity allowed-roles requested-pool-id]
  (let [roles-for-pool (if requested-pool-id
                         (->> auth-entity
                              (filter #(= (:inventory_pool_id %) requested-pool-id))
                              (map (comp keyword :role))
                              set)
                         (->> auth-entity
                              (map (comp keyword :role))
                              set))]
    (when-not (not-empty (clojure.set/intersection allowed-roles roles-for-pool))
      (throw (ex-info "invalid role for the requested pool or method" {:status 404})))
    roles-for-pool))

(defn permission-by-role-and-pool
  "Middleware that validates the user's roles, scopes, and optionally pool ID."
  [allowed-roles]
  (fn [handler]
    (fn [{{:keys [access-rights]} :authenticated-entity
          {{pool-id :pool_id} :path} :parameters
          :as request}]
      (try
        (let [roles-for-pool (validate-request access-rights allowed-roles pool-id)
              request (if pool-id
                        (assoc request :roles-for-pool {:pool_id pool-id :roles roles-for-pool})
                        request)]
          (when (nil? access-rights)
            (throw (ex-info "unknown user" {:status 403})))
          (handler request))
        (catch Exception e
          (debug e)
          (error "EXCEPTION-DETAIL: " e)
          (status (response {:error (.getMessage e)}) (:status (.getData e))))))))
