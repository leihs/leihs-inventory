(ns leihs.inventory.server.resources.auth.auth-routes
  (:require
   [buddy.auth :refer [authenticated?]]
   [buddy.auth.backends.token :refer [jws-backend]]
   [buddy.auth.middleware :refer [wrap-authentication]]
   [buddy.sign.jwt :as jwt]
   [clojure.set]
   [cider-ci.open-session.bcrypt :refer [checkpw]]

   [clojure.string :as str]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.util.response :as response]
   [next.jdbc :as jdbc]
   [buddy.hashers :as hashers]
   [schema.core :as s])
  (:import (java.util Base64 UUID)))

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

;; Basic Authentication Handler
(defn basic-auth-handler [request]
  (let [auth-header (get-in request [:headers "authorization"])
        encoded-credentials (when auth-header
                              (second (re-find #"^Basic (.+)$" auth-header)))
        credentials (when encoded-credentials
                      (String. (.decode (Base64/getDecoder) encoded-credentials)))
        [username password] (str/split credentials #":")]
    (if (and (= username "admin") (= password "password"))
      {:status 200 :body {:token (generate-token username)}}
      {:status 401 :body "Invalid credentials"})))

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

(defn to-uuid [id]
  (UUID/fromString id))


;; Function to fetch hashed password from the database
(defn fetch-hashed-password [request username auth-system-id]
  (let [
        p (println ">o> fetch-hashed-password" )
        query "SELECT asu.data FROM authentication_systems_users asu
               JOIN users u ON u.id = asu.user_id
               WHERE u.login = ? AND asu.id = ?"
        result (jdbc/execute-one! (:tx request) [query username (to-uuid auth-system-id)])
        p (println ">o> result=" result)
        ]
    (:data result)))

;; Function to verify password
(defn verify-password [request username password auth-system-id]
  (if-let [hashed-password (fetch-hashed-password request username auth-system-id)]
    (try
      (println ">o> check=" password hashed-password)
      (hashers/check password hashed-password)
      (catch Exception e
        (println ">o> check=" e)
        false
      ))
    false))


(defn verify-password [request username password auth-system-id]
  (if-let [hashed-password (fetch-hashed-password request username auth-system-id)]
    (try
      ;; Print the hashed password and ensure it's in bcrypt format
      (println ">o> check password=" password "hashed-password=" hashed-password)
      ;; Ensure the password is checked against a valid bcrypt hash
      (if (and (string? hashed-password)
            (re-matches #"\$2[abxy]\$\d+\$.*" hashed-password)) ;; Check for valid bcrypt format
        (hashers/check password hashed-password)
        (do
          (println ">o> Invalid bcrypt hash format" hashed-password)
          false))
      (catch Exception e
        (println ">o> check exception=" e)
        false))
    false))

(defn utf8 [s]
  (String. (.getBytes s "UTF-8")))

(defn verify-password [request username password auth-system-id]
  (if-let [hashed-password (fetch-hashed-password request username auth-system-id)]
    (try
      (println ">o> check password=" password "hashed-password=" hashed-password)
      ;; Convert both password and hashed-password to UTF-8
      (let [utf8-password (utf8 password)
            utf8-hashed-password (utf8 hashed-password)]
        ;(hashers/check utf8-password utf8-hashed-password) ;;broken
        (checkpw password hashed-password)                  ;;works
        )
      (catch Exception e
        (println ">o> check exception=" e)
        false))
    false))


(defn pr [str fnc]
  (println ">oo> HELPER / " str fnc)
  fnc
  )

;; Handler to authenticate user
(defn authenticate-handler [request]
  (let [{:keys [username password auth-system-id]} (:body-params request)
        ;; d86d4c53-8afc-4d78-8663-635b01df9fdf
        p (println ">o> auth=" username password auth-system-id)
        ]
    (if (pr "verify?" (verify-password request username password auth-system-id))
      (response/response {:status "success" :message "User authenticated successfully"})
      (response/status (response/response {:status "failure" :message "Invalid credentials"}) 401))))

;; Define the authentication route
;(defn auth-routes []
;  [["/authenticate"
;    {:post {:description "Authenticate user with username and password."
;            :parameters {:body {:username s/Str
;                                :password s/Str
;                                :auth-system-id s/Str}} ;; Include authentication_system_id for identifying which system to check
;            :handler authenticate-handler}}]])

;; Define authentication routes
(defn token-routes []
  [["/"
    {:tags ["Login process"]}
    ["login"
     {:post {
             :description "Login with username and password. (admin / password)"
             :accept "application/json"
             :coercion reitit.coercion.schema/coercion
             :swagger {:security [{:basicAuth []}]}
             ;:parameters {:body {:username s/Str :password s/Str}}
             :handler basic-auth-handler
             :responses {200 {:description "OK" :body s/Any}
                         401 {:description "Unauthorized"}
                         500 {:description "Internal Server Error"}}}}]
    [["public" {:get hello-handler}]
     ["protected" {:get {
                         :description "Use 'Token &lt;token&gt;' as Authorization header."
                         :accept "application/json"
                         :coercion reitit.coercion.schema/coercion
                         :swagger {:security [{:BearerAuth []}]}
                         :handler protected-handler
                         :middleware [wrap-jwt-auth]}}]



     ["/authenticate"
       {:post {
               :accept "application/json"
               :coercion reitit.coercion.schema/coercion
               :description "Authenticate user with username and password. 'd86d4c53-8afc-4d78-8663-635b01df9fdf'"
               :parameters {:body {:username s/Str
                                   :password s/Str
                                   :auth-system-id s/Uuid}} ;; Include authentication_system_id for identifying which system to check
               :handler authenticate-handler}}]

     ]]])

