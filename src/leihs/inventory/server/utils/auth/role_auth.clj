(ns leihs.inventory.server.utils.auth.role-auth
  (:require
   [clojure.string :as str]
   [leihs.inventory.server.utils.helper :refer [safe-ex-data]]
   [ring.util.response :refer [response status]]
   [taoensso.timbre :refer [error]]))

(defn determine-required-scope
  "Determines the required scope based on the HTTP method and URI."
  [method uri]
  (cond
    (and (str/includes? uri "/admin/") (= method :get)) :scope_system_admin_read
    (and (str/includes? uri "/admin/") (#{:post :put :delete} method)) :scope_system_admin_write
    (= method :get) :scope_read
    (#{:post :patch :put :delete} method) :scope_write))

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
      (throw (ex-info "invalid role for the requested pool or method" {:status 404})))
    roles-for-pool))
