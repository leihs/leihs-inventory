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
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.constants :refer [HIDE_BASIC_ENDPOINTS APPLY_DEV_ENDPOINTS]]
   ;[leihs.inventory.server.resources.auth.session :as ab]
   [leihs.inventory.server.resources.utils.request :refer [AUTHENTICATED_ENTITY authenticated? get-auth-entity]]
   [next.jdbc :as jdbc]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.util.response :as response]
   [schema.core :as s])
  (:import (com.google.common.io BaseEncoding)
           (java.time Duration Instant)
           (java.util Base64 UUID)))

(defn fetch-hashed-password [request login]
  (let [query (->
               (sql/select :users.id :users.login :authentication_systems_users.authentication_system_id :authentication_systems_users.data)
               (sql/from :authentication_systems_users)
               (sql/join :users [:= :users.id :authentication_systems_users.user_id])
               (sql/where [:= :users.login login]
                          [:= :authentication_systems_users.authentication_system_id "password"])
               sql-format)
        result (jdbc/execute-one! (:tx request) query)]
    (:data result)))

(defn verify-password [request login password]

  (if (or (nil? login) (nil? password))
    false
    (if-let [user (fetch-hashed-password request login)]
      (try
        (let [hashed-password user
              res (checkpw password hashed-password)]
          res)
        (catch Exception e
          (println "Error in verify-password:" e)
          false))
      false)))

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
                                            (second (re-find #"(?i)^Basic (.+)$" auth-header)))
                      credentials (when encoded-credentials
                                    (String. (.decode (Base64/getDecoder) encoded-credentials)))
                      [login password] (str/split credentials #":")]
                  (vector login password)))]
      res)
    (catch Exception e
      (throw
       (ex-info "BasicAuth header not found."
                {:status 403})))))

(defn update-role-handler [request]
  (try
    (let [[login password] (extract-basic-auth-from-header request)
          user (verify-password-entry request login password)
          default-pool-id #uuid "8bd16d45-056d-5590-bc7f-12849f034351"
          pool-id (or (-> request :parameters :query :pool_id) default-pool-id)
          role (or (-> request :parameters :query :role))
          auth (-> request :authenticated-entity)]
      (if (-> auth boolean)
        (let [access-rights (-> auth :access-rights)
              access-right (vec (filter #(= (:inventory_pool_id %) pool-id) access-rights))
              result {:inventory_pool_id pool-id
                      :role-before (-> access-right first :role)
                      :role-after role}]
          (if (nil? access-right)
            (response/status (response/response {:status "failure" :message "Invalid credentials"}) 403)
            (let [query (-> (sql/select :direct_access_rights.*)
                            (sql/from :direct_access_rights)
                            (sql/join :users [:= :users.id :direct_access_rights.user_id])
                            (sql/join :inventory_pools [:= :inventory_pools.id :direct_access_rights.inventory_pool_id])
                            (sql/where [:= :users.login (-> auth :login)]
                                       [:= :users.is_admin true]
                                       [:= :inventory_pools.id pool-id])
                            sql-format)
                  query-result (jdbc/execute! (:tx request) query)
                  count-of-query-should-be-one (count query-result)
                  dar-id (-> (first query-result) :id)
                  result (assoc result :count-of-direct-access-right-should-be-one count-of-query-should-be-one)
                  result (when (= count-of-query-should-be-one 1)
                           (let [update-query (-> (sql/update :direct_access_rights)
                                                  (sql/set {:role (-> result :role-after)})
                                                  (sql/where [:= :id dar-id])
                                                  (sql/returning :*)
                                                  sql-format)
                                 update-result (jdbc/execute! (:tx request) update-query)]
                             (assoc result :update-result update-result)))]
              (if (= count-of-query-should-be-one 1)
                (response/response result)
                (response/status (response/response result) 409)))))
        (response/status (response/response {:status "failure" :message "Invalid credentials"}) 403)))
    (catch Exception e
      (println "Error in authenticate-handler:" (.getMessage e))
      (response/status (response/response {:message (.getMessage e)}) 400))))

(defn logout-handler [request]
  (let [user-id (-> request :authenticated-entity :id)]
    (try
      (let [delete-query (-> (sql/delete-from :user_sessions)
                             (sql/where [:= :user_id user-id])
                             sql-format)
            delete-result (jdbc/execute! (:tx request) delete-query)]
        (if (> (:next.jdbc/update-count (first delete-result)) 0)
          (do
            (log/info "Successfully removed session for user_id:" user-id)
            (response/response {:status "success" :message "User logged out successfully"}))
          (do
            (log/warn "No session found for user_id:" user-id)
            (response/status
             (response/response {:status "failure" :message "No active session found"}) 404))))
      (catch Exception e
        (log/error "Error in logout-handler:" e)
        (response/status (response/response {:message (.getMessage e)}) 400)))))

;; ------------------------------------------------------

(defn set-password [request login password]
  (let [hashed-password (hashpw password)
        query "UPDATE authentication_systems_users
               SET data = ?
               WHERE user_id = (SELECT id FROM users WHERE login = ?)"]
    (jdbc/execute-one! (:tx request) [query hashed-password login])))

(defn set-password-handler [request]
  (try
    (let [{:keys [new-password1]} (:body-params request)
          login (-> request :authenticated-entity :login)]
      (if authenticated?
        (do
          (set-password request login new-password1)
          (response/response {:status "success" :message "Password updated successfully"}))
        (response/status
         (response/response {:status "failure" :message "Invalid credentials"}) 401)))

    (catch Exception e
      (println "Error updating password: " e)
      (response/status
       (response/response
        {:message "Error updating password"})
       400))))

;(defn password-hash
;  ([password tx]
;   (-> (sql/select [[:crypt password [:gen_salt "bf"]] :pw_hash])
;       sql-format
;       (->> (jdbc/execute-one! tx))
;       :pw_hash)))

(defn public-endpoint-handler [request]
  {:status 200
   :body {:reuqest-method (:request-method request)
          :request-url (:uri request)
          :message "Hello, World!"}})

(defn protected-handler [request]
  (if (authenticated? request)
    (do
      (println "User authenticated with:" (get-auth-entity request))
      {:status 200
       :body {:message "Access granted to protected resource"
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
        token-part (subs full-token 0 5)
        hashed-token (hashpw full-token)

        now-raw (java.time.Instant/now)
        now (java.sql.Timestamp/from now-raw)

        expires-at (.plus now-raw (java.time.Duration/ofDays 30))
        expires-sql (java.sql.Timestamp/from expires-at)

        data ["INSERT INTO api_tokens
                     (user_id, token_hash, token_part, scope_read, scope_write, scope_admin_read, scope_admin_write,
                      description, created_at, updated_at, expires_at)
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
              expires-sql]

        res (try (jdbc/execute-one! (:tx request) data)
                 (catch Exception e (println "Error inserting token:" e) nil))]
    {:token full-token
     :expires_at expires-at
     :scopes scopes}))

(defn create-api-token-handler [request]
  (let [user (-> request :authenticated-entity)
        {:keys [description scopes]} (:body-params request)
        user_id (:id user)
        scopes (merge {:read true :write false :admin_read false :admin_write false} scopes)]

    (if user_id
      (let [result (create-api-token request user_id scopes description)]
        (response/response
         {:status "success"
          :token (:token result)
          :expires_at (:expires_at result)
          :scopes scopes}))
      (response/status
       (response/response {:status "failure" :message "Invalid or missing credentials"})
       401))))

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
                                                              token-part
                                                              current-time])]
                         query-result)
                       nil)

        query-result (when (not (nil? query-result))
                       (let [token_hash (:token_hash query-result)
                             res (verify-with :bcrypt token token_hash)]
                         (if res
                           query-result
                           nil)))]
    (if query-result
      {:id (:user_id query-result)
       :scopes (extract-scope-attributes query-result)
       :expires_at (:expires_at query-result)}
      nil)))

(defn wrap-token-authentication
  "Middleware that checks if the token is valid."
  [handler]
  (fn [request]
    (let [tx (:tx request)
          header (get-in request [:headers "authorization"])
          token (when header (clojure.string/replace header "Token " ""))

          verification-result (verify-token tx token)]
      (if verification-result
        (handler (assoc request AUTHENTICATED_ENTITY verification-result))
        (response/status (response/response {:status "failure" :message "Unauthorized"}) 401)))))
;
;(defn session-token-routes []
;  [["/"
;
;    {:no-doc HIDE_BASIC_ENDPOINTS}
;
;    ["session"
;     {:tags ["Auth / Session"]
;      }
;
;     ["/public"
;      {:get {:swagger {:security []}
;             :handler public-endpoint-handler}}]
;     ["/protected"
;      {:get {:accept "application/json"
;             :coercion reitit.coercion.schema/coercion
;             :swagger {:security [{:csrfToken []}]}
;             :handler protected-handler
;             :middleware [ab/wrap]}}]]
;
;    ["token"
;     {:tags ["Auth / Token"]
;      }
;
;     ["/"
;      {:post {:summary "Create an API token with creds for a user"
;              :description "Generates an API token for a user with specific permissions and scopes"
;              :accept "application/json"
;              :coercion reitit.coercion.schema/coercion
;              :parameters {:body {:description s/Str
;                                  :scopes {:read s/Bool
;                                           :write s/Bool
;                                           :admin_read s/Bool
;                                           :admin_write s/Bool}}}
;              :handler create-api-token-handler}}]
;
;     ["/public"
;      {:get {:swagger {:security []}
;             :handler public-endpoint-handler}}]
;
;     ["/protected"
;      {:get {:description "Use 'Token &lt;token&gt;' as Authorization header."
;             :accept "application/json"
;             :coercion reitit.coercion.schema/coercion
;             :swagger {:security [{:apiAuth []}]}
;             :handler protected-handler
;             :middleware [wrap-token-authentication]}}]]]])
