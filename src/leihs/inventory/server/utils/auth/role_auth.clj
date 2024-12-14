(ns leihs.inventory.server.utils.auth.role-auth
  (:require
   [ring.util.response :refer [response status]]
   [leihs.inventory.server.utils.auth.roles :as roles]))

;; Helper function to validate request
(defn validate-request
  "Validates the user's access based on roles, scope, and optionally a pool ID."
  [auth-entity allowed-roles requested-pool-id]
  (let [roles-for-pool (if requested-pool-id
                         ;; Filter roles for the specific pool
                         (->> auth-entity
                           (filter #(= (:inventory_pool_id %) requested-pool-id))
                           (map (comp keyword :role))
                           set)
                         ;; Use all roles if no pool ID is specified
                         (->> auth-entity
                           (map (comp keyword :role))
                           set))]
    ;; Validation logic
    (when-not (not-empty (clojure.set/intersection allowed-roles roles-for-pool))
      (throw (Exception. "invalid role for the requested pool or method")))
    roles-for-pool))

;; Middleware: permission-by-role
(defn permission-by-role
  "Middleware that blocks requests if the user's roles do not match the allowed roles and scopes."
  [allowed-roles]
  (fn [handler]
    (fn [request]
      (try
        (let [auth-entity (get-in request [:authenticated-entity :access-rights])
              _ (when (nil? auth-entity)
                  (throw (Exception. "unknown user")))
              method (get request :request-method)

              ;; Map HTTP methods to required scopes
              required-scope (case method
                               :get :scope_read
                               (:post :put :delete) :scope_write)

              has-scope? (get-in request [:authenticated-entity required-scope])]

          (when-not has-scope?
            (throw (Exception. "invalid scope for the requested method")))

          ;; Validate roles
          (validate-request auth-entity allowed-roles nil)

          ;; Proceed to handler
          (handler request))

        (catch Exception e
          (println ">o> error in permission-by-role" (.getMessage e))
          (status (response {:error (str "Unauthorized: " (.getMessage e))}) 401))))))

;; Middleware: permission-by-role-and-pool
(defn permission-by-role-and-pool
  "Middleware that blocks requests if the user's roles, inventory pool ID, and request method
  do not match the allowed roles, pools, and scopes."
  [allowed-roles]
  (fn [handler]
    (fn [request]
      (try
        (let [auth-entity (get-in request [:authenticated-entity :access-rights])
              _ (when (nil? auth-entity)
                  (throw (Exception. "unknown user")))
              requested-pool-id (get-in request [:parameters :path :pool_id])
              method (get request :request-method)



              ;; todo: add further checks based on users.is_admin & users.is_system_admin
              ;:scope_admin_read (:is_admin user)
              ;:scope_admin_write (:is_admin user)
              ;:scope_system_admin_read (:is_system_admin user)
              ;:scope_system_admin_write (:is_system_admin user))))



              ;; Map HTTP methods to required scopes
              required-scope (case method
                               :get :scope_read
                               (:post :put :delete) :scope_write)

              has-scope? (get-in request [:authenticated-entity required-scope])]

          (when-not has-scope?
            (throw (Exception. "invalid scope for the requested method")))

          ;; Validate roles and pool
          (validate-request auth-entity allowed-roles requested-pool-id)

          ;; Proceed to handler
          (handler request))

        (catch Exception e
          (println ">o> error in permission-by-role-and-pool" (.getMessage e))
          (status (response {:error (str "Unauthorized: " (.getMessage e))}) 401))))))
