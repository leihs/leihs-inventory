(ns leihs.inventory.server.utils.auth.role-auth
  (:require
   [ring.util.response :refer [response status]]
   [clojure.string :as str]
   [leihs.inventory.server.utils.auth.roles :as roles]))

;; Helper: Determine required scope based on HTTP method and URI
(defn determine-required-scope
  "Determines the required scope based on the HTTP method and URI."
  [method uri]
  (cond
    (and (str/includes? uri "/admin/") (= method :get)) :scope_system_admin_read
    (and (str/includes? uri "/admin/") (#{:post :put :delete} method)) :scope_system_admin_write
    (= method :get) :scope_read
    (#{:post :put :delete} method) :scope_write))

;; Helper: Validate admin scopes
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

;; Helper: Validate request
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
                           set))
        p (println ">o> roles-for-pool" roles-for-pool)
        ]
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
        (let [user (get-in request [:authenticated-entity])
              auth-entity (:access-rights user)
              _ (when (nil? auth-entity)
                  (throw (Exception. "unknown user")))
              method (get request :request-method)
              uri (get request :uri)

              ;; Determine required scope
              required-scope (determine-required-scope method uri)

              has-scope? (or (get user required-scope)
                           (validate-admin-scopes user required-scope))

              _ (when-not has-scope?
                  (throw (Exception. "invalid scope for the requested method")))

              ;; Validate roles and pool
              roles-for-pool (validate-request auth-entity allowed-roles nil)
              p (println ">o> roles-for-pool" roles-for-pool)

              ]
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
        (let [user (get-in request [:authenticated-entity])
              auth-entity (:access-rights user)
              _ (when (nil? auth-entity)
                  (throw (Exception. "unknown user")))
              method (get request :request-method)
              uri (get request :uri)
              requested-pool-id (get-in request [:parameters :path :pool_id])

              ;; Determine required scope
              required-scope (determine-required-scope method uri)

              has-scope? (or (get user required-scope)
                           (validate-admin-scopes user required-scope))

          _ (when-not has-scope?
            (throw (Exception. "invalid scope for the requested method")))

          ;; Validate roles and pool
              roles-for-pool (validate-request auth-entity allowed-roles requested-pool-id)
              p (println ">o> roles-for-pool" roles-for-pool)

              request (assoc request :roles-for-pool {:pool_id requested-pool-id :roles roles-for-pool})

              ]

          ;; Proceed to handler
          (handler request))

        (catch Exception e
          (println ">o> error in permission-by-role-and-pool" (.getMessage e))
          (status (response {:error (str "Unauthorized: " (.getMessage e))}) 401))))))

