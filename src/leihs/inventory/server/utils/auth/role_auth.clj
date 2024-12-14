(ns leihs.inventory.server.utils.auth.role-auth
  (:require
   ;[ring.util.http-response :as response]
   [ring.util.response :refer [bad-request response status]]
   [leihs.inventory.server.utils.auth.roles :as roles]))

(defn permission-by-role
  "Middleware that blocks requests if the user's roles do not match the allowed roles."
  [allowed-roles]
  (fn [handler]
    (fn [request]
      (println ">o> permission-by-role")
      (let [auth-entity (get-in request [:authenticated-entity :access-rights])
            user-roles (set (map :role auth-entity))]
        (if (not-empty (clojure.set/intersection allowed-roles user-roles))
          ;; User has at least one allowed role, proceed to handler
          (handler request)
          ;; No matching roles, return bad request
          ;(bad-request {:error "Bad Request: Insufficient permissions"}))))))
(status (response {:error "Unauthorized: Insufficient permissions"}) 401))))))

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


            p (println ">o> abc" (nil? auth-entity) method)

            ;; Map HTTP methods to required scopes
            required-scope (case method
                             :get :scope_read
                             (:post :put :delete) :scope_write)
            p (println ">o> required-scope" required-scope)

            ;; Normalize roles to keywords
            roles-for-pool (->> auth-entity
                             (filter #(= (:inventory_pool_id %) requested-pool-id))
                             (map (comp keyword :role))
                             set)
            p (println ">o> roles-for-pool" roles-for-pool)

            ;; Check if the user has the required scope
            has-scope? (get-in request [:authenticated-entity required-scope])
            p (println ">o> has-scope?" has-scope?)

            ]

        ;; Debugging information
        (println ">o> auth-entity" auth-entity)
        (println ">o> requested-pool-id" requested-pool-id)
        (println ">o> roles-for-pool" roles-for-pool)
        (println ">o> method" method)
        (println ">o> required-scope" required-scope)
        (println ">o> has-scope?" has-scope?)

        ;; Validation logic
        (if (and has-scope? ;; User has the required scope
              (not-empty (clojure.set/intersection allowed-roles roles-for-pool))) ;; Allowed roles match
          ;; User has the right role and scope for the specific pool, proceed
          (handler request)
          ;; Unauthorized for this pool, role, or method
          ;(status (response {:error "Unauthorized: Insufficient permissions for the requested inventory pool or method"}) 401))
          (.throw (Exception. "invalid role or scope"))
          )




        )

      (catch Exception e
        ;(println ">o> abc" e)
          ;(status (response {:error "Unauthorized: Insufficient permissions for the requested inventory pool or method"}) 401))
          (status (response {:error (str "Unauthorized: Insufficient permissions, " (.getMessage e))}) 401))
      )

      )))
