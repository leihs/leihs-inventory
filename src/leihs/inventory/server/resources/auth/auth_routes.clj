(ns leihs.inventory.server.resources.auth.auth-routes
  (:require
   [buddy.auth.backends.token :refer [jws-backend]]
   [buddy.auth.middleware :refer [wrap-authentication]]

   [buddy.hashers :as hashers]
   [buddy.sign.jwt :as jwt]

   [cider-ci.open-session.bcrypt :refer [checkpw hashpw]]

   [clojure.data.codec.base64 :as b64]


   [clojure.data.json :as json]

   [clojure.pprint :refer [pprint]]
   [clojure.set]


   [clojure.string :as str]
   ;[camel-snake-kebab.core :refer :all]
   ;[cider-ci.open-session.bcrypt :refer [checkpw]]
   ;[clojure.walk :refer [keywordize-keys]]
   ;[inflections.core :refer :all]
   ;[madek.api.authentication.token :as token-authentication]
   ;[madek.api.resources.shared.core :as sd]
   ;[next.jdbc :as jdbc]

   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.auth.session :as session]
   [leihs.core.constants :as c]
   [leihs.core.core :refer [str]]
   [next.jdbc :as jdbc]
   ;[taoensso.timbre :refer [debug warn]])


   [pandect.algo.sha256 :as algo.sha256]
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
  ;(jwt/sign {:user-id user-id} secret {:alg :hs256}))
  (jwt/sign {:user-id user-id} secret {:alg :hs256 :iterations 6}))


;; Generate bcrypt hash with a specific cost factor (06 in this case)
(defn generate-bcrypt-hash [token]
  ;(hashers/derive token {:alg :bcrypt+sha512 :iterations 6}))
  (hashers/derive token {:alg :hs256}))



(defn generate-token2 [user-id]
  (println ">o> generate-token.user-id=" (str ">" user-id "<"))
  (jwt/sign {:user-id user-id} secret {:alg :hs256}))

;; Helper function to convert a string to UUID
(defn to-uuid [id]
  (println ">o> try to-uuid" id)
  (try
    (UUID/fromString id)

    (catch Exception e
      (println ">o> to-uuid FAILED!!" id)
      id
      )
    )
  )

(defn hash-token [token]
  (hashers/derive token))

(defn hash-token2 [token]
  ;(hashers/derive token)

  (hashers/derive token {:alg :bcrypt :cost 6}))



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
  (if-let [auth-header (get-in request [:headers "authorization"])]
    (let [
          p (println ">o> auth-header=" auth-header)

          encoded-credentials (when auth-header
                                (second (re-find #"^Basic (.+)$" auth-header)))
          credentials (when encoded-credentials
                        (String. (.decode (Base64/getDecoder) encoded-credentials)))
          [username password] (str/split credentials #":")

          p (println ">o> extract-basic-auth=" username password)
          ]
      (vector username password)
      )
    [nil nil]

    ))


(defn extract-token-auth-from-header [request]
  (if-let [auth-header (get-in request [:headers "authorization"])]
    (let [
          p (println ">o> auth-header=" auth-header)

          encoded-credentials (when auth-header
                                (second (re-find #"^Token (.+)$" auth-header)))
          ;credentials (when encoded-credentials
          ;              (String. (.decode (Base64/getDecoder) encoded-credentials)))
          ;[username password] (str/split credentials #":")

          p (println ">o> encoded-credentials=" encoded-credentials)
          ]
      encoded-credentials
      )
    nil

    ))




;
;    p (println ">o> auth-header=" auth-header)
;
;    encoded-credentials (when auth-header
;                          (second (re-find #"^Basic (.+)$" auth-header)))
;    credentials (when encoded-credentials
;                  (String. (.decode (Base64/getDecoder) encoded-credentials)))
;    [username password] (str/split credentials #":")
;
;    p (println ">o> extract-basic-auth=" username password)
;    ]
;;{:username username :password password}
;
;;[username password]
;(vector username password)
;)
;)

;; Basic Authentication Handler
(defn basic-auth-handler [request]
  (let [
        [username password] (extract-basic-auth-from-header request)
        p (println ">o> auth=" username password)

        verfication-entry-result (if (and (nil? username) (nil? password))
                                   nil
                                   (let [
                                         user-entry (verify-password-entry request username password)

                                         p (println ">o> verfication-ok=" user-entry)

                                         query "SELECT t.* FROM api_tokens t
               JOIN users u ON u.id = t.user_id
               WHERE u.login = ?"
                                         result (jdbc/execute-one! (:tx request) [query username])

                                         p (println ">o> result1=" result username)

                                         result (if (nil? result)
                                                  (do
                                                    (println ">o> create token")
                                                    (let [
                                                          token (generate-token2 (:id user-entry))
                                                          p (println ">o> token=" token)

                                                          user-id (:id user-entry)

                                                          insert-query (-> (sql/insert-into :api_tokens)
                                                                           (sql/columns :user_id :token_hash :token_part)
                                                                           (sql/values [[user-id token "todo"]])
                                                                           (sql/returning :token_hash)
                                                                           (sql/returning :*)
                                                                           sql-format
                                                                           )

                                                          result (jdbc/execute! (:tx request) insert-query)
                                                          p (println ">o> token_hash=" result)

                                                          result (assoc (first result)
                                                                        :token token
                                                                        )

                                                          ]

                                                      ;token
                                                      result

                                                      )
                                                    )
                                                  result)
                                         p (println ">o> result2=" result username)


                                         token result
                                         ;
                                         ;p (println ">o> data=" data)
                                         ;token (generate-token2 data)
                                         ] token))
        ]
    ;(if (and (= username "admin") (= password "password"))
    (if verfication-entry-result
      {:status 200 :body {:token verfication-entry-result}}
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

        p (println ">o> auth1")

        [username password] (extract-basic-auth-from-header request)

        p (println ">o> auth2" username password)
        ]






    (if (verify-password request username password)
      (let [

            ; fetch user_id from db by username
            query "SELECT * FROM users u WHERE u.login = ?"
            user (jdbc/execute-one! (:tx request) [query username])
            p (println ">o> user=" user)

            authentication-system-id "password"

            sign-in-token nil
            ;sign-in-token (create-user-session user "password" request)  ;; Create a session for the user
            user-session (session/create-user-session
                           user authentication-system-id request
                           :user-session (select-keys sign-in-token [:external_session_id]))

            ;token (generate-token username)                 ;; Generate JWT token

            p (println ">o> user-session=" user-session)

            ;; TODO
            cookie {:value user-session
                    :http-only true
                    :secure false                           ;; Make sure to use HTTPS for secure cookies
                    :max-age 3600                           ;; Set cookie expiration to 1 hour
                    :path "/inventory"}                     ;; Cookie available for all routes
            ;:path "/"         }                      ;; Cookie available for all routes

            ]

        ;; Return the response with the session cookie
        (-> (response/response {:status "success" :message "User authenticated successfully"

                                :body {c/USER_SESSION_COOKIE_NAME cookie}

                                :session {c/USER_SESSION_COOKIE_NAME cookie}
                                })
            ;(response/set-cookie "session-token" cookie)))  ;; Set the cookie in response

            ;; session-token only
            (response/set-cookie c/USER_SESSION_COOKIE_NAME cookie)


            ;(response/set-cookie "_leihs_session" cookie)   ;;

            ))                                              ;; Set the cookie in response
      ;; If authentication fails
      (response/status (response/response {:status "failure" :message "Invalid credentials"}) 401))))


;value=authentication_system_id=password%26token_hash=478e1941619c67c3232e78d59704a71131e9057d6c8814b77bc3b83bae9102d6%26meta_data=user_agent%3DMozilla%2F5.0%20%28Macintosh%3B%20Intel%20Mac%20OS%20X%2010_15_7%29%20AppleWebKit%2F537.36%20%28KHTML%2C%20like%20Gecko%29%20Chrome%2F128.0.0.0%20Safari%2F537.36

(defn get-leihs-user-session [request]
  ;; Access the cookie from the request
  ;(try
  (let [

        ;session-cookie (get-in request [:cookies "wtf"])
        ;p (println ">o> session-cookie.wtf" session-cookie)

        ;cookie-value (get-in request [:cookies "value"])
        ;p (println ">o> cookie-value=??" cookie-value)

        ;cookie-value (get-in request [:Cookies "value"])
        ;p (println ">o> cookie-value2=??" cookie-value)


        ;p (println ">o> request ???" request)

        session-cookie (:cookies request)
        p (println ">o> session-cookie ???" session-cookie)


        session-cookie (get-in request [:cookies "leihs-user-session"])
        p (println ">o> session-cookie" session-cookie)
        ]
    ;(if session-cookie
    ;  ;; Respond with the value of the "leihs-user-session" cookie
    ;  (response/response (str "Session cookie value: " (:value session-cookie)))
    ;  ;; If the cookie is not found, respond accordingly
    ;  (response/response "No session cookie found"))
    session-cookie
    )


  ;(catch Exception e
  ;  (println ">o> " e)
  ;  )

  )
;)


(defn pr [str fnc]
  ;(println ">oo> HELPER / " str fnc)(println ">oo> HELPER / " str fnc)
  (println ">oo> " str fnc)
  fnc
  )

(defn is-admin [user-id tx]
  (let [result (->
                (pr "isAdminRes=" (jdbc/execute!
                                    tx
                                    (-> (sql/select :is_system_admin)
                                        (sql/from :users)
                                        (sql/where [:= :id (to-uuid user-id)])
                                        sql-format))
                  )
                first
                :is_system_admin
                ;empty?
                )
        p (println ">o> is-admin=" result)

        ;result (not none)
        ]
    ;(info "is-admin: " user-id " : " result)
    result))

(defn- get-api-client-by-login [login tx]
  (->> (jdbc/execute! tx (-> (sql/select :*) (sql/from :api_clients) (sql/where [:= :login login]) sql-format))
    (map #(assoc % :type "ApiClient"))
    first))

(defn- get-user-by-login-or-email-address [login-or-email tx]
  (->> (jdbc/execute! tx (-> (sql/select :*)
                             (sql/from :users)
                             (sql/where [:or [:= :login login-or-email] [:= :email login-or-email]])
                             sql-format))
    (map #(assoc % :type "User"))
    (map #(clojure.set/rename-keys % {:email :email_address}))
    first))

(defn get-entity-by-login-or-email [login-or-email tx]
  (or (get-api-client-by-login login-or-email tx)
    (get-user-by-login-or-email-address login-or-email tx)))

(defn- get-auth-systems-user [userId tx]
  (jdbc/execute-one! tx (-> (sql/select :*)
                            (sql/from :authentication_systems_users)
                            (sql/where [:= :user_id userId] [:= :authentication_system_id "password"])
                            sql-format)))

;(defn user-password-authentication [login-or-email password handler request]
(defn user-password-authentication [login-or-email request]
  (let [tx (:tx request)
        ;entity (get-entity-by-login-or-email login-or-email tx)
        entity (get-user-by-login-or-email-address login-or-email tx)
        p (println ">o> entity??" entity)

        asuser (when entity (get-auth-systems-user (:id entity) tx))
        p (println ">o> asuser??" asuser)

        ]


    (try
      (cond
        (not entity) {:status 401 :body {:message (str "Neither User nor ApiClient exists for "
                                                       {:login-or-email-address login-or-email})}}
        (nil? (get asuser :data)) {:status 401 :body "Only password auth users supported for basic auth."}
        ;(or (nil? password) (not (checkpw password (:data asuser)))) {:status 401 :body {:message (str "Password mismatch for "
        ;                                                                                               {:login-or-email-address login-or-email})}}
        :else (assoc request
                     :authenticated-entity entity
                     :is_admin (is-admin (or (:id entity) (:user_id entity)) tx)
                     :authentication-method "Basic Authentication???"))
      (catch Exception e
        (println ">o> " e)

        )

      )))


(defn parse-session-string [session-string]
  (let [; Remove the initial ">o> session.val2= "
        cleaned-string (str/replace session-string ">o> session.val2= " "")
        ; Split the string into key-value pairs by "&"
        key-value-pairs (str/split cleaned-string #"&")
        ; Helper function to split "key=value" into a map entry
        parse-pair (fn [pair]
                     (let [[k v] (str/split pair #"=" 2)]
                       [(keyword k) v]))
        ; Create a base map from key-value pairs
        session-map (into {} (map parse-pair key-value-pairs))
        ; Special handling for "meta_data", which is a nested map
        meta-data (get session-map :meta_data)
        parsed-meta (into {} (map parse-pair (str/split meta-data #"&")))]
    ; Merge the parsed meta_data back into the map
    (assoc session-map :meta_data parsed-meta)))


(defn uuid-from-string [uuid-string]
  (UUID/fromString uuid-string))

(defn print-cookies-handler
  [request]
  (let [cookies (:cookies request)]                         ;; Extract cookies from the request
    (println "Cookies:")
    (pprint cookies)                                        ;; Print the cookies in the console
    ;(response/response (str "Cookies: " (pr-str cookies)))
    ))


(defn print-headers-handler
  [request]
  (let [headers (:headers request)]                         ;; Extract the headers map
    (println "Headers:")
    (pprint headers)                                        ;; Print all headers nicely in the console
    ;(response/response (str "Headers printed to console: " (pr-str headers)))
    ))


;; Middleware to authenticate user by session
(defn wrap-authenticate-by-session [handler]
  (fn [request]
    ;; Print headers and cookies for debugging
    (print-headers-handler request)
    (print-cookies-handler request)

    ;; Extract and parse the session cookie
    (let [session (get-leihs-user-session request)
          _ (println ">o> session1" session)
          request (when session
                    (let [_ (println ">o> session.val=" session)
                          _ (println ">o> session.val2=" (:value session))
                          session (parse-session-string (:value session))
                          _ (println ">o> session.val4=" (:user_id session))

                          ;; Fetch user by session user_id
                          query "SELECT * FROM users u WHERE u.id = ?"
                          user (jdbc/execute-one! (:tx request) [query (uuid-from-string (:user_id session))])
                          _ (println ">o> user=" user)

                          ;; Authenticate user based on login
                          request (user-password-authentication (:login user) request)
                          _ (println ">o> auth-method=" (:authentication-method request))
                          _ (println ">o> auth-isAdmin=" (:is_admin request))

                          request (assoc request
                                         ;:authenticated-entity entity
                                         ;:is_admin (is-admin (or (:id entity) (:user_id entity)) tx)
                                         ;:authentication-method "Basic Authentication???"
                                         :session session
                                         )
                          ]

                      ;; Return the session if everything succeeds
                      ;session))]
                      request))]

      ;; If session is valid, proceed to the next handler
      (if (:session request)
        (handler request)
        ;; Invalid session, return 401 Unauthorized
        (response/status (response/response {:status "failure"
                                             :message "Invalid session-credentials"}) 401)))))





(defn authenticate-by-session-handler [request]
  ;; Print headers and cookies for debugging
  (print-headers-handler request)
  (print-cookies-handler request)

  ;; Extract session from cookies
  (let [session (get-leihs-user-session request)]
    (if session
      (do
        (println ">o> session1")
        (let [parsed-session (parse-session-string (:value session))
              user-id (:user_id parsed-session)]
          (println ">o> session.val=")
          (println ">o> session.val4=" user-id)

          ;; Fetch user by session user_id
          (if-let [user (jdbc/execute-one! (:tx request) ["SELECT * FROM users u WHERE u.id = ?" (uuid-from-string user-id)])]
            (do
              (println ">o> user=" user)

              ;; Perform user authentication
              (let [request-with-auth (user-password-authentication (:login user) request)]
                (println ">o> auth-method=" (:authentication-method request-with-auth))
                (println ">o> auth-isAdmin=" (:is_admin request-with-auth))

                ;; If everything succeeds, return success
                (println ">o> return success")
                (response/response {:status "success"})))
            ;; User not found, return failure
            (do
              (println ">o> User not found")
              (response/status (response/response {:status "failure"
                                                   :message "Invalid session-credentials"}) 401)))))
      ;; Session is nil, return failure
      (do
        (println ">o> Session not found")
        (response/status (response/response {:status "failure"
                                             :message "Invalid session-credentials"}) 401)))))


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


;; Function for Base64 decoding
(defn base64-decode [encoded-str]
  (let [decoder (Base64/getDecoder)]
    (String. (.decode decoder encoded-str) "UTF-8")))

(defn protected-handler [request]

  (let [

        token (extract-token-auth-from-header request)

        is-token-correct (if token
                           (let [
                                 decoded-token (base64-decode token)
                                 p (println ">o> decoded-token=" decoded-token)

                                 decoded-map (json/read-str decoded-token :key-fn keyword)
                                 p (println ">o> decoded-map (JSON to Map)" decoded-map)


                                 p (println ">o> protected-handler.token" token)


                                 ; fetch user_id from db by username
                                 query "SELECT t.* FROM users u  JOIN
      api_tokens t ON u.id = t.user_id
        WHERE u.login = ? AND u.id = ?"
                                 api_token_entry (jdbc/execute-one! (:tx request) [query (:login decoded-map) (to-uuid (:user-id decoded-map))])

                                 p (println ">o> token_hash=" (:token_hash api_token_entry))
                                 is-token-correct (checkpw token (:token_hash api_token_entry))
                                 ]
                             is-token-correct
                           )
                           nil)



      ;  decoded-token (base64-decode token)
      ;  p (println ">o> decoded-token=" decoded-token)
      ;
      ;  decoded-map (json/read-str decoded-token :key-fn keyword)
      ;  p (println ">o> decoded-map (JSON to Map)" decoded-map)
      ;
      ;
      ;  p (println ">o> protected-handler.token" token)
      ;
      ;
      ;  ; fetch user_id from db by username
      ;  query "SELECT t.* FROM users u  JOIN
      ;api_tokens t ON u.id = t.user_id
      ;  WHERE u.login = ? AND u.id = ?"
      ;  api_token_entry (jdbc/execute-one! (:tx request) [query (:login decoded-map) (to-uuid (:user-id decoded-map))])
      ;
      ;  p (println ">o> token_hash=" (:token_hash api_token_entry))
      ;  is-token-correct (checkpw token (:token_hash api_token_entry))
        ]

    ;(if (authenticated? request)
    (if is-token-correct
      (do
        (println "User authenticated with:" (:identity request))
        {:status 200 :body "Access granted to protected resource"})
      (do
        (println "User not authenticated")
        {:status 403 :body "Forbidden"}))

    )
  )



;(sql/where [:in :api_tokens.token_hash
;            (->> secrets
;              (filter identity)
;              (map hash-string))])

(defn ^String base64-encode [^bytes bts]
  (String. (.encode (Base64/getEncoder) bts)))
(defn hash-string [s]
  (->> s
    algo.sha256/sha256-bytes
    base64-encode))



(defn find-user-token-by-some-secret [secrets tx]

  (println ">o> find-user-token-by-some-secret >> " (->> secrets
                                                      (filter identity)
                                                      (map hash-string)))


  (let [

        res (try (->> (-> (sql/select :users.*
                            [:scope_read :token_scope_read]
                            [:scope_write :token_scope_write]
                            ;[:revoked :token_revoked]
                            [:description :token_description])
                          (sql/from :api_tokens)
                          (sql/where [:in :api_tokens.token_hash
                                      (->> secrets
                                        (filter identity)
                                        (map hash-string))])
                          ;(sql/where [:<> :api_tokens.revoked true])
                          (sql/where [:raw "now() < api_tokens.expires_at"])
                          (sql/join :users [:= :users.id :api_tokens.user_id])
                          (sql-format))
                   (jdbc/execute! tx)
                   )
                 (catch Exception e (println ">o> find-user-token-by-some-secret.exception=" e) nil))

        p (println ">o> find.res=" res)
        ] res)








  ;(->> (-> (sql/select :users.*
  ;           [:scope_read :token_scope_read]
  ;           [:scope_write :token_scope_write]
  ;           [:revoked :token_revoked]
  ;           [:description :token_description])
  ;         (sql/from :api_tokens)
  ;         (sql/where [:in :api_tokens.token_hash
  ;                     (->> secrets
  ;                       (filter identity)
  ;                       (map hash-string))])
  ;         (sql/where [:<> :api_tokens.revoked true])
  ;         (sql/where [:raw "now() < api_tokens.expires_at"])
  ;         (sql/join :users [:= :users.id :api_tokens.user_id])
  ;         (sql-format))
  ;  (jdbc/execute! tx)
  ;  (map #(clojure.set/rename-keys % {:email :email_address}))
  ;  first)

  )

;;; Create an api_token record in the database
;(defn create-api-token [request user-id password scopes description]
;  (let [
;        p (println ">o> create-api-token ------------------")
;        p (println ">o> user-id=" user-id)
;        p (println ">o> scopes=" scopes)
;        p (println ">o> description=" description)
;
;
;        ;_ (println "hashpw.test / 12345 = " (hashpw "12345"))
;
;
;        ;{:keys [username password]} (:body-params request)
;        ;
;        ;
;        ;verfication-entry-result (verify-password-entry request username password)
;        ;
;        ;
;        ;{:keys [full-token token-part]} (generate-token (:id verfication-entry-result))
;        ;full-token (generate-token user-id) ;;not working
;
;        full-token (hashpw password)
;
;        p (println ">o> full-token.res=" full-token)
;
;        ;token-part (subs full-token 0 10)
;        token-part (subs full-token 0 5)
;        p (println ">o> token-part=" token-part)
;
;        p (println ">o> hash-token=" (hash-token full-token))
;
;
;        p (println ">o> abc1" (hash-string full-token))
;        p (println ">o> abc2" (hash-string "abc"))
;        ;p (println ">o> abc3" (hash-string "abc"))
;
;        ;p (println ">o> hash-token2=" (try (hash-token2 full-token) (catch Exception e
;        ;                                                              (println ">o> insert-token.exception=" e)
;        ;                                                              nil)))
;        ;hashed-token (hash-token full-token)
;
;
;
;        ;hashed-token (generate-bcrypt-hash full-token)
;        ;;hashed-token (hashpw full-token)                    ;; TODO
;        ;p (println ">o> hashed-token=" hashed-token)
;
;        hashed-token (hashpw full-token)                    ;; TODO
;        p (println ">o> hashed-token2=" hashed-token)
;
;
;
;        now (Instant/now)
;        expires-at (.plus (Instant/now) (Duration/ofDays 365))
;
;        ;["INSERT INTO api_tokens (user_id, token_hash, token_part, scope_read, scope_write, scope_admin_read, scope_admin_write, description, created_at, updated_at, expires_at)
;        ;VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
;
;        insert-token (try (jdbc/execute-one! (:tx request)
;                            ["INSERT INTO api_tokens (user_id, token_hash, token_part, scope_read, scope_write, scope_admin_read, scope_admin_write)
;        VALUES (?, ?, ?, ?, ?, ?, ?)"
;                             user-id hashed-token token-part
;                             (:read scopes) (:write scopes) (:admin_read scopes) (:admin_write scopes)
;                             ;description now now expires-at
;                             ])
;                          (catch Exception e
;                            (println ">o> insert-token.exception=" e)
;                            nil
;                            ))
;        p (println ">o> insert-token.res=" insert-token)
;
;
;        secrets full-token
;
;        ;find-result (find-user-token-by-some-secret [password] (:tx request))
;        ;p (println ">o> ???? find-result1=" find-result)
;
;        find-result (find-user-token-by-some-secret password (:tx request))
;        p (println ">o> ???? find-result2=" find-result)
;
;        ]
;    ;; Insert token into database
;
;    {:token full-token                                      ;; Return full token to the user
;     :expires_at expires-at
;     :scopes scopes}))


(defn ^String base64-encode [^bytes bts]
  (String. (.encode (Base64/getEncoder) bts)))

;; Function for Base64 encoding
(defn base64-encode [s]
  (let [encoder (Base64/getEncoder)]
    (.encodeToString encoder (.getBytes s "UTF-8"))))

;; Function to generate a random token, Base64-encode it, and hash it with BCrypt
(defn generate-random-bcrypt-token [user-id login]
  (try
    (let [p (println ">o> params" user-id login)

          ;; Create a raw token by converting a map to a JSON string
          raw-token (json/write-str {:user-id user-id :login login})
          p (println ">o> raw-token (JSON)" raw-token)

          ;; Encode the raw token in Base64
          random-token (base64-encode raw-token)
          p (println ">o> random-token (Base64)" random-token)

          ;; Hash the Base64-encoded token with BCrypt
          ;hashed-token (hashers/derive random-token {:alg :bcrypt :cost 6})
          hashed-token (hashpw random-token)
          p (println ">o> hashed-token (BCrypt)" hashed-token)

          ;; Get the first 5 characters of the Base64 token for the token part
          token-part (subs random-token 0 5)]

      ;; Return the results
      {:raw-token raw-token
       :full-token random-token
       :hashed-token hashed-token
       :token-part token-part})

    (catch Exception e
      (println ">o> generate-random-bcrypt-token.exception=" e)
      nil)))

;; Create an api_token record in the database
(defn create-api-token [request user-id username password scopes description]
  (let [p (println ">o> create-api-token ------------------")
        p (println ">o> user-id=" user-id)
        p (println ">o> scopes=" scopes)
        p (println ">o> description=" description)

        ;; Step 1: Generate a random token and hash it using BCrypt
        generated-result (generate-random-bcrypt-token user-id username)
        {:keys [full-token hashed-token token-part]} generated-result

        p (println ">o> full-token=" full-token)
        p (println ">o> hashed-token=" hashed-token)
        p (println ">o> token-part=" token-part)

        ;;; Get the token-part (substring of the full token)
        ;token-part (subs full-token 0 5)
        ;p (println ">o> token-part=" token-part)

        ;; Step 2: Prepare timestamps for now and expiration
        now (Instant/now)
        expires-at (.plus now (Duration/ofDays 365))

        ;; Step 3: Insert the token into the database
        insert-token (try
                       (jdbc/execute-one! (:tx request)
                         ["INSERT INTO api_tokens (user_id, token_hash, token_part, scope_read, scope_write, scope_admin_read, scope_admin_write, description)
                           VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
                          user-id hashed-token token-part
                          (:read scopes) (:write scopes) (:admin_read scopes) (:admin_write scopes)
                          description

                          ;now now expires-at

                          ])
                       (catch Exception e
                         (println ">o> insert-token.exception=" e)
                         nil))

        p (println ">o> insert-token.res=" insert-token)
        ]


    generated-result

    ;; Step 4: Return the full token (random) and token details
    ;{:token full-token             ;; Return full (unhashed) token to the user
    ; :expires_at expires-at
    ; :scopes scopes}
    )                                                       ;; Return the scopes and expiration time
  )








;; API token handler
(defn create-api-token-handler [request]
  (let [
        ;{:keys [user_id description scopes]} (:body-params request)
        p (println ">o> create-api-token-handler")

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
      (let [result (create-api-token request user_id username password scopes description)
            p (println ">o> after.create-api-token result=" result)
            ]
        (response/response {:status "success"
                            ;:token (:token result)          ;; Full token returned here
                            ;:expires_at (:expires_at result)
                            :token result
                            :scopes scopes}))
      (response/status (response/response {:status "failure" :message "Missing user_id"}) 400))))
;
;(defn extract [request]
;  (try (when-let [auth-header (-> request :headers keywordize-keys :authorization)]
;         (when (re-matches #"(?i)^basic\s+.+$" auth-header)
;           (let [decoded-val (base64-decode (last (re-find #"(?i)^basic (.*)$" auth-header)))
;                 [username password] (clojure.string/split (str decoded-val) #":" 2)]
;             {:username username :password password})))
;       (catch Exception _
;         (println "failed to extract basic-auth properties because" _))))


;(defn create-api-token-handler [request]
;  (println ">o> create-api-token-handler slim")
;
;  (response/response {:status "success"})
;
;  )

(defn extract-basic-auth [headers]
  (let [auth-header (get headers "authorization")]
    (when (and auth-header (str/starts-with? auth-header "Basic "))
      (let [encoded-creds (subs auth-header 6)              ; remove "Basic "
            decoded-creds (String. (b64/decode (.getBytes encoded-creds))) ; decode Base64
            [username password] (str/split decoded-creds #":")]
        {:username username :password password}))))


(defn pr [str fnc]
  ;(println ">oo> HELPER / " str fnc)(println ">oo> HELPER / " str fnc)
  (println ">oo> " str fnc)
  fnc
  )

(defn create-session [request]
  (let [

        p (println ">o> create-session")

        ;{:keys [username password]} (request )

        ;res (extract request)
        res (extract-basic-auth (:headers request))
        p (println ">o> res=" res)

        username (:username res)
        password (:password res)

        p (println ">o> username1=" username)
        p (println ">o> password2=" password)

        ;https://cljdoc.org/d/buddy/buddy-hashers/1.4.0/doc/buddy-hashers-password-hashers-for-clojure
        hashed-password (try
                          ;(hashers/derive password {:alg :bcrypt :cost 10})
                          (pr "hashpw" (hashpw password))
                          ;(pr "default" (hashers/derive password { :cost 10}))
                          ;(pr "brypt" (hashers/derive password {:alg :bcrypt+sha512 :cost 10}))
                          ;(pr "pure-brypt" (hashers/derive password {:alg :bcrypt :cost 10}))

                          (catch Exception e
                            (println ">o> hash exception=" e)
                            ))





        ;;hashpw (hashpw password)
        p (println ">o> hashed-password" hashed-password)
        ;
        result (let [query (-> (sql/update :authentication_systems_users)
                               (sql/set {:data hashed-password})
                               (sql/from [:users])
                               (sql/where [:=
                                           :authentication_systems_users.user_id
                                           :users.id])
                               (sql/where [:= :users.login username]))
                     sql-query (sql-format query)]
                 (jdbc/execute! (:tx request) sql-query))

        p (println ">o> result=" result)
        ;
        ;
        ;
        ;p (println ">o> username=" username)
        ;p (println ">o> password=" password)
        ;
        ;
        ;
        ;
        ;p (println ">o> verify.res=" (verify-password request username password))
        ]
    (if (verify-password request username password)
      (do
        (response/response {:status "success" :message "User authenticated successfully"}))
      (response/status (response/response {:status "failure" :message "Invalid credentials"}) 401))

    ;(response/response {:status "success" :message "???"})

    )

  )


(defn dev-auth-routes []
  [["/"

    ["token"
     {:tags ["Auth / Token (DEV)"]}

     ["/create"
      {:post {:summary "OK | Create an API token for a user - BROKEN / api_tokens"
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
              :swagger {:security [{:basicAuth []}]

                        :deprecated true

                        }
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
     ["/token-protected" {:get {
                                :summary "OK | Token contains user_id and login (base64)"
                                :description "Use 'Token &lt;token&gt;' as Authorization header."
                                :accept "application/json"
                                :coercion reitit.coercion.schema/coercion
                                :swagger {:security [{:BearerAuth []} {:SessionAuth []}]}
                                :handler protected-handler
                                ;:middleware [wrap-jwt-auth]

                                }}]

     ["/both-protected" {:get {
                               :description "Can be used by both Bearer and Session Auth."
                               :accept "application/json"
                               :coercion reitit.coercion.schema/coercion
                               :swagger {:security [{:BearerAuth []} {:SessionAuth []}]}
                               :handler protected-handler
                               :middleware [wrap-jwt-auth]}}]
     ]


    ;; --------------------------------------------------------------------------------------

    [""
     {:tags ["Auth / Session (DEV)"]}


     ;;; Route to authenticate user
     ;["session-authenticate"
     ; {:post {
     ;        :summary "OK | Authenticate user by login and set session ( and fetch token ) ADD: basicAuth"
     ;        :accept "application/json"
     ;        ;:description "Authenticate user with username and password. (bcrypt)  d86d4c53-8afc-4d78-8663-635b01df9fdf"
     ;        :description "Authenticate by login, create cookie and set session"
     ;        :coercion reitit.coercion.schema/coercion
     ;        :swagger {:security [{:basicAuth []}]}
     ;        :handler authenticate-handler}}]

     ["session/test-session-authentication"
      ;["test_session_authentication"
      ;["testSessionAuthentication"
      {:get {
             ;:summary "?? | Authenticate user by login and set session ( and fetch token ) ADD: basicAuth"
             :accept "application/json"


             :description "Use endpoint with session"

             ;:description "Authenticate user with username and password. (bcrypt)  d86d4c53-8afc-4d78-8663-635b01df9fdf"
             :coercion reitit.coercion.schema/coercion

             ;:middleware [wrap-cookies]

             ;:swagger {:security [{
             ;                      ;:basicAuth []
             ;                      :SessionAuth []}]}

             :handler authenticate-by-session-handler}}]
     ]


    ["auth"
     {:tags ["Auth / Login process"]}

     ; Route to authenticate user
     ["/create-session"
      {:post {

              ;:summary "OK | Authenticate user by login ( and fetch token ) ADD: basicAuth"
              ;:summary "OK | Set new password by login ( and fetch token ) | DB::authentication_systems_users"
              :summary "DEPR | ???"
              :accept "application/json"
              ;:description "Authenticate user with username and password. (bcrypt)  d86d4c53-8afc-4d78-8663-635b01df9fdf"
              :coercion reitit.coercion.schema/coercion
              :swagger {:security [{:basicAuth []}]
                        :deprecated true
                        }
              :handler create-session}}]


     ;; Route to set/update the password
     ["/set-password"
      {:post {
              :summary "OK | Set password by login OR token,  ADD: basicAuth & token"
              :accept "application/json"
              :description "Set or update the user's password. (bcrypt)"
              :coercion reitit.coercion.schema/coercion
              :swagger {:security [{:basicAuth []}]}
              :parameters {:body {:new-password1 s/Str}}
              :handler set-password-handler}}]
     ]

    ]])
