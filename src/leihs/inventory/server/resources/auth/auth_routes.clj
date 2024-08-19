(ns leihs.inventory.server.resources.auth.auth-routes
  (:require
   [buddy.auth :refer [authenticated?]]
   [buddy.auth.backends.token :refer [jws-backend]]
   [buddy.auth.middleware :refer [wrap-authentication]]
   [buddy.hashers :as hashers]
   [buddy.sign.jwt :as jwt]
   [cider-ci.open-session.bcrypt :refer [checkpw hashpw]]
   [clojure.set]
   [clojure.string :as str]
   [next.jdbc :as jdbc]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.util.response :as response]
   [schema.core :as s])
  (:import (java.time Duration Instant)
           (java.util Base64 UUID)))

;; JWT secret key and backend setup
(def secret "my-secret-key")

(def auth-backend
  (jws-backend {:secret secret :alg :hs256}))

;; JWT authentication middleware
(defn wrap-jwt-auth [handler]
  (wrap-authentication handler auth-backend))

;; Token generation
(defn generate-token [user-id]
  (println ">o> generate-token.user-id=" (str ">" user-id "<"))
  (jwt/sign {:user-id user-id} secret {:alg :hs256}))


;; Generate bcrypt hash with a specific cost factor (06 in this case)
(defn generate-bcrypt-hash [token]
  ;(hashers/derive token {:alg :bcrypt+sha512 :iterations 6}))
  (hashers/derive token {:alg :hs256}))



(defn generate-token2 [user-id]
  (println ">o> generate-token.user-id=" (str ">" user-id "<"))
  (jwt/sign {:user-id user-id} secret {:alg :hs256}))

;; Helper function to convert a string to UUID
(defn to-uuid [id]
  (UUID/fromString id))

(defn hash-token [token]
  (hashers/derive token))

;; Function to fetch hashed password from the database
(defn fetch-hashed-password [request username]
  (let [query "SELECT asu.data FROM authentication_systems_users asu
               JOIN users u ON u.id = asu.user_id
               WHERE u.login = ? AND asu.authentication_system_id = 'password'"
        result (jdbc/execute-one! (:tx request) [query username])]
    (:data result)))

;; Function to verify password
(defn verify-password [request username password]
  (if-let [hashed-password (fetch-hashed-password request username)]
    (try
      (println ">o> check password=" password "hashed-password=" hashed-password)
      ;; Use the `checkpw` function from bcrypt to verify the password
      (checkpw password hashed-password)
      (catch Exception e
        (println ">o> check exception=" e)
        false))
    false))


(defn verify-password-entry [request username password]
  (let [
        verfication-ok (verify-password request username password)

        ;; fetch user_id from db by username
        query "SELECT * FROM users u WHERE u.login = ?"
        result (jdbc/execute-one! (:tx request) [query username])
        ]

    (if verfication-ok
      result
      nil

      )
    )

  )


(defn extract-basic-auth-from-header [request]
  (let [auth-header (get-in request [:headers "authorization"])
        encoded-credentials (when auth-header
                              (second (re-find #"^Basic (.+)$" auth-header)))
        credentials (when encoded-credentials
                      (String. (.decode (Base64/getDecoder) encoded-credentials)))
        [username password] (str/split credentials #":")

        p (println ">o> extract-basic-auth=" username password)
        ]
    ;{:username username :password password}

    ;[username password]
    (vector username password)
    ))

;; Basic Authentication Handler
(defn basic-auth-handler [request]
  (let [

        ;auth-header (get-in request [:headers "authorization"])
        ;encoded-credentials (when auth-header
        ;                      (second (re-find #"^Basic (.+)$" auth-header)))
        ;credentials (when encoded-credentials
        ;              (String. (.decode (Base64/getDecoder) encoded-credentials)))
        ;[username password] (str/split credentials #":")


        [username password] (extract-basic-auth-from-header request)

        p (println ">o> auth=" username password)


        verfication-entry-result (verify-password-entry request username password)

        p (println ">o> verfication-ok=" verfication-entry-result)

        query "SELECT t.* FROM api_tokens t
               JOIN users u ON u.id = t.user_id
               WHERE u.login = ?"
        result (jdbc/execute-one! (:tx request) [query username])

        ;_ (if nil? result
        ;           (do
        ;             (println ">o> result is nil")
        ;             (response/status (response/response {:status "failure" :message "Invalid credentials"}) 401)
        ;             ;;
        ;
        ;             ))


        p (println ">o> result=" result)
        data result

        p (println ">o> data=" data)
        token (generate-token2 data)

        ]
    ;(if (and (= username "admin") (= password "password"))
    (if verfication-entry-result
      {:status 200 :body {:token token}}
      {:status 401 :body "Invalid credentials"})))

;; Handler to authenticate user
(defn authenticate-handler [request]
  (let [{:keys [username password auth-system-id]} (:body-params request)]
    (if (verify-password request username password)
      (response/response {:status "success" :message "User authenticated successfully"})
      (response/status (response/response {:status "failure" :message "Invalid credentials"}) 401))))


;; ---------- SESSION COOKIE HANDLING ----------

(defn create-cookie [cookie-name token-value]
  "Creates a session cookie with the given token value."
  {:value token-value
   :http-only true                                          ;; Prevent access to cookie via JavaScript (XSS protection)
   :secure true                                             ;; Only send cookie over HTTPS (important for production)
   :same-site :strict                                       ;; Prevent the browser from sending this cookie along with cross-site requests
   :path "/"                                                ;; Cookie is valid for the entire site
   :max-age 3600})                                          ;; Cookie expires in 1 hour (3600 seconds)

;; Handler to authenticate user and set session cookie
(defn authenticate-handler [request]
  (let [

        ;{:keys [username password auth-system-id]} (:body-params request)


        [username password] (extract-basic-auth-from-header request)
        ]



    (if (verify-password request username password)
      (let [token (generate-token username)                 ;; Generate JWT token
            cookie {:value token
                    :http-only true
                    :secure true                            ;; Make sure to use HTTPS for secure cookies
                    :max-age 3600                           ;; Set cookie expiration to 1 hour
                    :path "/"}]                             ;; Cookie available for all routes
        ;; Return the response with the session cookie
        (-> (response/response {:status "success" :message "User authenticated successfully"})
            (response/set-cookie "session-token" cookie)))  ;; Set the cookie in response
      ;; If authentication fails
      (response/status (response/response {:status "failure" :message "Invalid credentials"}) 401))))


;; ------------------------------------------------------

;; Function to update the hashed password in the database
(defn set-password [request username password]
  (let [
        ;hashed-password (hashers/derive password)  ;; Hash the plain password
        hashed-password (hashpw password)                   ;; Hash the plain password
        query "UPDATE authentication_systems_users
               SET data = ?
               WHERE user_id = (SELECT id FROM users WHERE login = ?)"]
    (jdbc/execute-one! (:tx request) [query hashed-password username])))

;; Handler for setting the user's password
(defn set-password-handler [request]
  (let [

        {:keys [new-password1]} (:body-params request)

        [username password] (extract-basic-auth-from-header request)

        ]
    (try

      (if (verify-password request username password)
        (do
          (set-password request username new-password1)
          (response/response {:status "success" :message "Password updated successfully"}))
        (response/status (response/response {:status "failure" :message "Invalid credentials"}) 401)
        )

      (catch Exception e
        (println "Error updating password: " e)
        (response/status (response/response {:status "failure" :message "Error updating password"}) 500))
      )))

;; Route handlers
(defn hello-handler [request]
  {:status 200 :body "Hello, World!"})


(defn protected-handler [request]
  (if (authenticated? request)
    (do
      (println "User authenticated with:" (:identity request))
      {:status 200 :body "Access granted to protected resource"})
    (do
      (println "User not authenticated")
      {:status 403 :body "Forbidden"})))



;; Create an api_token record in the database
(defn create-api-token [request user-id scopes description]
  (let [

        p (println ">o> user-id=" user-id)
        p (println ">o> scopes=" scopes)
        p (println ">o> description=" description)


        ;{:keys [username password]} (:body-params request)
        ;
        ;
        ;verfication-entry-result (verify-password-entry request username password)
        ;
        ;
        ;{:keys [full-token token-part]} (generate-token (:id verfication-entry-result))
        full-token (generate-token user-id)
        p (println ">o> res=" full-token)

        token-part (subs full-token 0 10)

        p (println ">o> abc??" (hash-token full-token))
        ;hashed-token (hash-token full-token)
        hashed-token (generate-bcrypt-hash full-token)
        ;hashed-token (hashpw full-token)                    ;; TODO
        p (println ">o> hashed-token=" hashed-token)

        hashed-token (hashpw full-token)                    ;; TODO
        p (println ">o> hashed-token2=" hashed-token)



        now (Instant/now)
        expires-at (.plus (Instant/now) (Duration/ofDays 365))]
    ;; Insert token into database
    (jdbc/execute-one! (:tx request)
      ["INSERT INTO api_tokens (user_id, token_hash, token_part, scope_read, scope_write, scope_admin_read, scope_admin_write, description, created_at, updated_at, expires_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
       user-id hashed-token token-part
       (:read scopes) (:write scopes) (:admin_read scopes) (:admin_write scopes)
       description now now expires-at])
    {:token full-token                                      ;; Return full token to the user
     :expires_at expires-at
     :scopes scopes}))

;; API token handler
(defn create-api-token-handler [request]
  (let [
        ;{:keys [user_id description scopes]} (:body-params request)


        [username password] (extract-basic-auth-from-header request)


        {:keys [description scopes]} (:body-params request)

        verfication-entry-result (verify-password-entry request username password)

        p (println ">o> auth=" username password)
        p (println ">o> auth2=" verfication-entry-result)

        user_id (:id verfication-entry-result)

        scopes (merge {:read true :write false :admin_read false :admin_write false} scopes)

        p (println ">o> scopes=" scopes)

        ]                                                   ;; Default scopes
    (if user_id
      (let [result (create-api-token request user_id scopes description)
            p (println ">o> result=" result)
            ]
        (response/response {:status "success"
                            :token (:token result)          ;; Full token returned here
                            :expires_at (:expires_at result)
                            :scopes scopes}))
      (response/status (response/response {:status "failure" :message "Missing user_id"}) 400))))



(defn token-routes []
  [["/"

    ["token"
     {:tags ["Auth / Token"]}

     [""
      {:post {:summary "Create an API token for a user - BROKEN / api_tokens"
              :description "Generates an API token for a user with specific permissions and scopes."
              :accept "application/json"
              :coercion reitit.coercion.schema/coercion
              :swagger {:security [{:basicAuth []}]}
              :parameters {:body {
                                  :description s/Str
                                  :scopes {:read s/Bool
                                           :write s/Bool
                                           :admin_read s/Bool
                                           :admin_write s/Bool}}}
              :handler create-api-token-handler}}
      ]

     ["/login"
      {:post {:summary "Authenticate user by login ( .. and fetch token ) ADD: basicAuth / api_tokens"
              :description "Login with username and password. (admin / password)"
              :accept "application/json"
              :coercion reitit.coercion.schema/coercion
              :swagger {:security [{:basicAuth []}]}
              :handler basic-auth-handler
              :responses {200 {:description "OK" :body s/Any}
                          401 {:description "Unauthorized"}
                          500 {:description "Internal Server Error"}}}}]


     ;; TODO: Create new session for one minute


     ;; TODO: Add a route to set/update the token-hashed password, api-token
     ;; /admin/token/create-new-token (latest one)
     ;; Generate new token by passing
     ;; 1. username & password
     ;; 2. token
     ;; 3. scope_read / scope_write (boolean)
     ;; 3. scope_admin_read / scope_admin_write (boolean)
     ;; 3. scope_system_admin_read / scope_system_admin_write (boolean)


     ["/public" {:get hello-handler}]
     ["/protected" {:get {
                          :description "Use 'Token &lt;token&gt;' as Authorization header."
                          :accept "application/json"
                          :coercion reitit.coercion.schema/coercion
                          :swagger {:security [{:BearerAuth []} {:SessionAuth []}]}
                          :handler protected-handler
                          :middleware [wrap-jwt-auth]}}]
     ]


    ;; --------------------------------------------------------------------------------------

    ["auth"
     {:tags ["Auth / Login process"]}

     ;; Route to authenticate user
     ["/authenticate"
      {:post {
              :summary "OK | Authenticate user by login ( and fetch token ) ADD: basicAuth"
              :accept "application/json"
              :description "Authenticate user with username and password. (bcrypt)  d86d4c53-8afc-4d78-8663-635b01df9fdf"
              :coercion reitit.coercion.schema/coercion
              :swagger {:security [{:basicAuth []}]}
              :handler authenticate-handler}}]

     ;; Route to set/update the password
     ["/set-password"
      {:post {
              :summary "OK | Set password by login OR token,  ADD: basicAuth & token"
              :accept "application/json"
              :description "Set or update the user's password. (bcrypt)"
              :coercion reitit.coercion.schema/coercion
              :swagger {:security [{:basicAuth []}]}
              :parameters {:body {:new-password1 s/Str}}
              :handler set-password-handler}}]]]])
