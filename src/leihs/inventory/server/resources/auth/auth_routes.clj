(ns leihs.inventory.server.resources.auth.auth-routes
  (:require

   [buddy.auth.backends.token :refer [jws-backend]]

   [buddy.auth.middleware :refer [wrap-authentication]]

   [buddy.sign.jwt :as jwt]
   [cider-ci.open-session.bcrypt :refer [checkpw hashpw]]
   [clojure.set]
   [clojure.string :as str]
   [clojure.test :refer :all]
   [clojure.tools.logging :as log]
   [crypto.random]
   [cryptohash-clj.api :refer :all]
   [digest :as d]

   [honey.sql :refer [format]
    :rename {format sql-format}]

   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.auth.session :as ab]
   [leihs.inventory.server.resources.utils.request :refer [AUTHENTICATED-ENTITY authenticated? get-auth-entity]]

   [next.jdbc :as jdbc]

   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.util.response :as response]
   [schema.core :as s])
  (:import (java.time Duration Instant)
           (java.util Base64 UUID)
           (com.google.common.io BaseEncoding)))

;; JWT secret key and backend setup
(def secret "my-secret-key")

(def auth-backend
  (jws-backend {:secret secret :alg :hs256}))

;; JWT authentication middleware
(defn wrap-jwt-auth [handler]
  (wrap-authentication handler auth-backend))

;; Token generation
(defn generate-token [user-id]
  (println ">o> generate-token.user2-id=" (str ">" user-id "<"))
  (jwt/sign {:user-id user-id} secret {:alg :hs256}))


;; Generate bcrypt hash with a specific cost factor (06 in this case)
(defn generate-bcrypt-hash [token]
  (hash-with :bcrypt token {:cpu-cost 6}))

(def PASSWORD_AUTHENTICATION_SYSTEM_ID "password")


(defn generate-token2 [user-id]
  (println ">o> generate-token.user1-id=" (str ">" user-id "<"))
  (jwt/sign {:user-id user-id} secret {:alg :hs256}))


(defn fetch-hashed-password [request login]
  (let [query "SELECT asu.data FROM authentication_systems_users asu
               JOIN users u ON u.id = asu.user_id
               WHERE u.login = ? AND asu.authentication_system_id = 'password'"
        result (jdbc/execute-one! (:tx request) [query login])]
    (:data result)))

(defn verify-password [request login password]
  (if-let [user (fetch-hashed-password request login)]
    (try
      (let [hashed-password user
            res (checkpw password hashed-password)]
        res)

      (catch Exception e
        (println ">o> check exception=" e)
        false))
    false))


(defn verify-password-entry [request login password]
  (let [verfication-ok (verify-password request login password)
        query "SELECT * FROM users u WHERE u.login = ?"
        result (jdbc/execute-one! (:tx request) [query login])]

    (if verfication-ok
      result
      nil)))


(defn extract-basic-auth-from-header [request]
  (try
    (let [auth-header (get-in request [:headers "authorization"])
          res (if (nil? auth-header)
                (vector nil nil)

                (let [encoded-credentials (when auth-header
                                            (second (re-find #"^Basic (.+)$" auth-header)))
                      credentials (when encoded-credentials
                                    (String. (.decode (Base64/getDecoder) encoded-credentials)))
                      [login password] (str/split credentials #":")]
                  (vector login password)

                  )

                )

          ]
      res
      )


    (catch Exception e
      (throw
        (ex-info "BasicAuth header not found."
          {:status 403})))))

(defn sha256-hash [token]
  (d/sha-256 token))

(defn authenticate-handler [request]
  (try
    (let [[login password] (extract-basic-auth-from-header request)
          user (verify-password-entry request login password)]
      (if user
        (let [
              token (str (UUID/randomUUID))
              hashed-token (sha256-hash token)

              auth-system-id "password"
              user-id (:id user)

              check-query (-> (sql/select :*)
                              (sql/from :user_sessions)
                              (sql/where [:= :user_id [:cast user-id :uuid]]
                                [:= :authentication_system_id auth-system-id])
                              sql-format)
              existing-session (jdbc/execute-one! (:tx request) check-query)]

          (when existing-session
            (println "!!! Session already exists for user:" user-id))

          (let [insert-query (-> (sql/insert-into :user_sessions)
                                 (sql/values [{:token_hash hashed-token
                                               :user_id user-id
                                               :authentication_system_id auth-system-id
                                               ;:expires_at expires-at
                                                }])
                                 sql-format)
                insert-res (jdbc/execute! (:tx request) insert-query)]
            (println "Inserted new session:" insert-res))

          (let [
                max-age 3600
                cookie {:http-only true
                        :secure true
                        :max-age max-age
                        :path "/"}]
            (-> (response/response
                  {:status "success" :message "User authenticated successfully"})
                (response/set-cookie "leihs-user-session" token cookie {:max-age max-age :path "/"})
                (response/set-cookie "leihs-session" user {:max-age max-age :path "/"})
                (response/set-cookie "leihs-anti-csrf-token" "NOT-IMPLEMENTED" {:max-age max-age :path "/"}))))

        (response/status
          (response/response {:status "failure" :message "Invalid credentials"}) 403)))

    (catch Exception e
      (println "Error in authenticate-handler:" (.getMessage e))
      (response/status
        (response/response {:message (.getMessage e)}) 400))))




(defn logout-handler [request]
  (let [token (get-in request [:cookies "leihs-user-session" :value])
        hashed-token (sha256-hash token)]
    (try
      (let [delete-query (-> (sql/delete-from :user_sessions)
                             (sql/where [:= :token_hash hashed-token])
                             sql-format)
            delete-result (jdbc/execute! (:tx request) delete-query)]
        (if (> (:next.jdbc/update-count (first delete-result)) 0) ;; Check if any row was affected
          (do
            (log/info "Successfully removed session for token:" token)
            (-> (response/response {:status "success" :message "User logged out successfully"})
                (response/set-cookie "leihs-session" "" {:max-age 0 :path "/"})
                (response/set-cookie "leihs-anti-csrf-token" "" {:max-age 0 :path "/"})
                ))
          (do
            (log/warn "No session found for token:" token)
            (response/status (response/response {:status "failure" :message "No active session found"}) 404))))

      (catch Exception e
        (println "Error in logout-handler:" e)
        (response/status (response/response {:message (.getMessage e)}) 400)))))

;; ------------------------------------------------------

;; Function to update the hashed password in the database
(defn set-password [request login password]
  (let [hashed-password (hashpw password)                   ;; Hash the plain password
        query "UPDATE authentication_systems_users
               SET data = ?
               WHERE user_id = (SELECT id FROM users WHERE login = ?)"]
    (jdbc/execute-one! (:tx request) [query hashed-password login])))


(defn fetch-and-print-entry [request login]
  (println "------------------------------------")
  (let [query "SELECT data FROM authentication_systems_users
               WHERE user_id = (SELECT id FROM users WHERE login = ?)"
        result (jdbc/execute-one! (:tx request) [query login])
        ]
    (when (seq result)
      (println "Retrieved entry:" (-> result :data))
      (println "------------------------------------"))))


(defn fetch-user-data [request login]
  (println "------------------------------------")
  (let [query "SELECT user_id, data, authentication_system_id FROM authentication_systems_users
               WHERE user_id = (SELECT id FROM users WHERE login = ?)"
        asu-result (jdbc/execute-one! (:tx request) [query login])

        query "SELECT id, login, firstname, lastname, email, is_system_admin, is_admin,
        admin_protected, pool_protected FROM users
               WHERE  login = ?"
        user-result (jdbc/execute-one! (:tx request) [query login])]
    {:user user-result
     :authentication_systems_users asu-result}))

(defn set-password-handler [request]
  (try
    (let [{:keys [new-password1]} (:body-params request)

          [login password] (extract-basic-auth-from-header request)]

      (if (verify-password request login password)
        (do
          (println ">o> set password1")
          (set-password request login new-password1)
          (println ">o> set password2")
          (response/response {:status "success" :message "Password updated successfully"}))
        (response/status
          (response/response {:status "failure" :message "Invalid credentials"}) 401)))

    (catch Exception e
      (println "Error updating password: " e)
      (response/status
        (response/response
          {:message "Error updating password"})
        400))))


(defn password-hash
  ([password tx]
   (-> (sql/select [[:crypt password [:gen_salt "bf"]] :pw_hash])
       sql-format
       (->> (jdbc/execute-one! tx))
       :pw_hash)))


(defn sql-command [login pw-hash tx]
  (println ">o> sql-command" login pw-hash)
  (let [subquery (-> (sql/select :id)
                     (sql/from :users)
                     (sql/where [:= :login login])
                     sql-format)

        res (jdbc/execute-one! tx subquery)
        p (println ">o> res11=" res)

        query (-> (sql/update :authentication_systems_users)
                  (sql/set {:data pw-hash})
                  (sql/where
                    [:and
                     [:= :user_id (:id res)]
                     [:= :authentication_system_id PASSWORD_AUTHENTICATION_SYSTEM_ID]])
                  (sql/returning :*))]
    (sql-format query)))

(defn set-password-original [login password tx]
  (println ">o> set-password-original")
  (let [pw-hash (password-hash password tx)
        sql-command (sql-command login pw-hash tx)
        res (jdbc/execute! tx sql-command)]
    res))

(defn reset-password [request]
  (let [{:keys [new-password login]} (:body-params request)

        ; variant #1
        _ (set-password request login new-password)
        _ (fetch-and-print-entry request login)
        verify-set-password (verify-password request login new-password)
        _ (println "verify1 =>" verify-set-password)


        ; variant #2
        _ (set-password-original login new-password (:tx request))
        _ (fetch-and-print-entry request login)
        verify-set-password-original (verify-password request login new-password)

        ]
    (try
      (if (verify-password request login new-password)
        (do
          (response/response
            {:status "success"
             :message "Password updated successfully"
             :verify-set-password verify-set-password
             :verify-set-password-original verify-set-password-original
             :data (fetch-user-data request login)}))
        (response/status
          (response/response {:status "failure" :message "Invalid credentials"}) 401))

      (catch Exception e
        (println "Error updating password: " e)
        (response/status
          (response/response {:status "failure" :message "Error updating password"}) 500)))))

;; Route handlers
(defn hello-handler [request]
  {:status 200 :body "Hello, World!"})


(defn protected-handler [request]
  (if (authenticated? request)
    (do
      (println "User authenticated with:" (get-auth-entity request))
      {:status 200 :body {:message "Access granted to protected resource"
                          :token (get-auth-entity request)}})
    (do
      (println "User not authenticated")
      {:status 403 :body "Forbidden"})))

(def b32 (BaseEncoding/base32))

(defn secret [n]
  (->> n crypto.random/bytes
    (.encode b32)
    (map char)
    (apply str)))

(defn create-api-token [request user-id scopes description]
  (let [full-token (secret 20)
        p (println ">o> res.token=" full-token)

        token-part (subs full-token 0 5)
        p (println ">o> res.token.sub=" token-part)

        hashed-token (hash-with :bcrypt full-token {:cpu-cost 6})
        p (println ">o> hashed-token8=" hashed-token)
        p (println ">o> hashed-token8c=" (verify-with :bcrypt full-token hashed-token))


        hashed-token (hash-with :bcrypt full-token {:cpu-cost 10})
        p (println ">o> hashed-token10=" hashed-token)
        p (println ">o> hashed-token10c=" (verify-with :bcrypt full-token hashed-token))

        hashed-token (generate-bcrypt-hash full-token)
        ;hashed-token (hashpw full-token)                    ;; TODO
        p (println ">o> hashed-token2=" hashed-token)
        p (println ">o> hashed-token2c=" (verify-with :bcrypt full-token hashed-token))

        ;; TODO: should create a random token
        ;https://github.com/leihs/leihs-admin/blob/1ba0da7b14dba9327c7253812602683957a646da/src/leihs/admin/resources/users/user/api_tokens/api_token/main.clj#L40-L44


        hashed-token (hashpw full-token)                    ;; TODO
        now-raw (java.time.Instant/now)                     ;; Current timestamp

        now (java.sql.Timestamp/from now-raw)

        expires-at (.plus now-raw (java.time.Duration/ofDays 30))

        expires-sql (java.sql.Timestamp/from expires-at)

        data ["INSERT INTO api_tokens
                     (user_id, token_hash, token_part, scope_read, scope_write, scope_admin_read, scope_admin_write, description, created_at, updated_at, expires_at)
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
              user-id
              hashed-token
              token-part
              (:read scopes)
              (:write scopes)
              (:admin_read scopes)
              (:admin_write scopes)
              description
              now
              now
              expires-sql
              ]

        res (try (jdbc/execute-one! (:tx request) data) (catch Exception e (println "Error inserting token:" e) nil))]
    {:token full-token
     :expires_at expires-at
     :scopes scopes}))

(defn create-api-token-handler [request]
  (let [[login password] (extract-basic-auth-from-header request)
        {:keys [description scopes]} (:body-params request)
        verfication-entry-result (verify-password-entry request login password)
        user_id (:id verfication-entry-result)
        scopes (merge {:read true :write false :admin_read false :admin_write false} scopes)]

    (if user_id
      (let [result (create-api-token request user_id scopes description)]
        (response/response
          {:status "success"
           :token (:token result)
           :expires_at (:expires_at result)
           :scopes scopes}))
      (response/status
        (response/response {:status "failure" :message "Invalid or missing credentials"}) 401))))


(defn extract-scope-attributes [data]
  (select-keys data (filter #(clojure.string/starts-with? (name %) "scope_") (keys data))))

(defn verify-token
  "Checks if the token is valid based on a database entry."
  [tx token]
  (let [now-raw (java.time.Instant/now)
        current-time (java.sql.Timestamp/from now-raw)
        query-result (if (not (nil? token))
                       (let [token-part (subs token 0 5)
                             query-result (jdbc/execute-one! tx
                                            ["SELECT * FROM api_tokens WHERE token_part = ? AND expires_at > ?"
                                             token-part current-time])
                             ] query-result)
                       nil
                       )

        query-result (when (not (nil? query-result))
                       (let [token_hash (:token_hash query-result)
                             res (verify-with :bcrypt token token_hash)]
                         (if res
                           query-result
                           nil)))]
    (if query-result
      {:id (:user_id query-result)
       :scopes (extract-scope-attributes query-result)
       :expires_at (:expires_at query-result)
       }
      nil)))

(defn wrap-token-authentication
  "Middleware that checks if the token is valid."
  [handler]
  (fn [request]
    (let [
          tx (:tx request)
          token (get-in request [:headers "authorization"])
          token (when token (clojure.string/replace token "Token " ""))

          verification-result (verify-token tx token)
          ]
      (if verification-result
        (handler (assoc request AUTHENTICATED-ENTITY verification-result))
        (response/status (response/response {:status "failure" :message "Unauthorized"}) 401)))))


(defn token-routes []
  [["/"

    ["session"
     {:tags ["Auth / Session"]}


     ["/public" {:get {:swagger {:security []}
                       :handler hello-handler}}]
     ["/protected"
      {:get {:description "Use 'Token &lt;token&gt;' as Authorization header."
             :accept "application/json"
             :coercion reitit.coercion.schema/coercion
             :swagger {:security []}
             :handler protected-handler
             :middleware [ab/wrap]}}]]

    ["token"
     {:tags ["Auth / Token"]}

     ["/"
      {:post {:summary "Create an API token with creds for a user"
              :description "Generates an API token for a user with specific permissions and scopes (login / password)"
              :accept "application/json"
              :coercion reitit.coercion.schema/coercion
              :swagger {:security [{:basicAuth []}]}
              :parameters {:body {:description s/Str
                                  :scopes {:read s/Bool
                                           :write s/Bool
                                           :admin_read s/Bool
                                           :admin_write s/Bool}}}
              :handler create-api-token-handler}}]

     ;; TODO: rewrite to fetch all tokens?
     ;["/login"
     ; {:get {:summary "Authenticate user by login ( .. and fetch token )"
     ;        :description "Login with login and password (login / password)"
     ;        :accept "application/json"
     ;        :coercion reitit.coercion.schema/coercion
     ;        :swagger {:security [{:basicAuth []}]}
     ;        :handler basic-auth-handler
     ;        :responses {200 {:description "OK" :body s/Any}
     ;                    401 {:description "Unauthorized"}
     ;                    500 {:description "Internal Server Error"}}}}]

     ;; TODO: Add a route to set/update the token-hashed password, api-token
     ;; /admin/token/create-new-token (latest one)
     ;; Generate new token by passing
     ;; 1. login & password
     ;; 2. token
     ;; 3. scope_read / scope_write (boolean)
     ;; 3. scope_admin_read / scope_admin_write (boolean)
     ;; 3. scope_system_admin_read / scope_system_admin_write (boolean)


     ["/public" {:get {:swagger {:security []}
                       :handler hello-handler}}]

     ["/protected"
      {:get {:description "Use 'Token &lt;token&gt;' as Authorization header."
             :accept "application/json"
             :coercion reitit.coercion.schema/coercion
             :swagger {:security [{:apiAuth []}]}
             :handler protected-handler
             :middleware [wrap-token-authentication
                          ]}}]]

    ]])