(ns leihs.inventory.server.utils.auth.role-auth
  (:require
   [clojure.string :as str]
   [leihs.inventory.server.utils.auth.roles :as roles]
   [ring.util.response :refer [response status]]))

(defn determine-required-scope
  "Determines the required scope based on the HTTP method and URI."
  [method uri]
  (cond
    (and (str/includes? uri "/admin/") (= method :get)) :scope_system_admin_read
    (and (str/includes? uri "/admin/") (#{:post :put :delete} method)) :scope_system_admin_write
    (= method :get) :scope_read
    (#{:post :put :delete} method) :scope_write))

(defn validate-admin-scopes
  "Checks admin-level scopes (is_admin or is_system_admin) for elevated privileges."
  [user required-scope]
  (cond
    (:is_system_admin user)
    (case required-scope
      :scope_system_admin_read true
      :scope_system_admin_write true
      true)

    (:is_admin user)
    (case required-scope
      :scope_admin_read true
      :scope_admin_write true
      true)

    :else
    false))

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
      (throw (Exception. "invalid role for the requested pool or method")))
    roles-for-pool))

(defn permission-by-role-and-pool
  "Middleware that validates the user's roles, scopes, and optionally pool ID."
  [allowed-roles]
  (fn [handler]
    (fn [request]
      (try
        (let [user (get-in request [:authenticated-entity])
              auth-entity (:access-rights user)
              _ (when (nil? auth-entity)
                  (throw (Exception. "unknown user")))
              method (get request :request-method)
              uri (get request :uri)
              requested-pool-id (get-in request [:parameters :path :pool_id])
              required-scope (determine-required-scope method uri)
              has-scope? (or (get user required-scope)
                             (validate-admin-scopes user required-scope))
              _ (when-not has-scope?
                  (throw (Exception. "invalid scope for the requested method")))

              roles-for-pool (validate-request auth-entity allowed-roles requested-pool-id)
              request (if requested-pool-id
                        (assoc request :roles-for-pool {:pool_id requested-pool-id :roles roles-for-pool})
                        request)]
          (handler request))

        (catch Exception e
          (println "EXCEPTION-DETAIL: in permission-checker" (.getMessage e))
          (status (response {:error (str "Unauthorized: " (.getMessage e))}) 401))))))
