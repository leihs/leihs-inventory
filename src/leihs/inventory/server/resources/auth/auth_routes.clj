(ns leihs.inventory.server.resources.auth.auth-routes
  (:require
   [buddy.auth :refer [authenticated?]]
   [buddy.auth.backends.token :refer [jws-backend]]
   [buddy.auth.middleware :refer [wrap-authentication]]
   [buddy.sign.jwt :as jwt]
   [clojure.set]
   [cider-ci.open-session.bcrypt :refer [checkpw hashpw]]
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

;; Helper function to convert a string to UUID
(defn to-uuid [id]
  (UUID/fromString id))

;; Function to fetch hashed password from the database
(defn fetch-hashed-password [request username auth-system-id]
  (let [query "SELECT asu.data FROM authentication_systems_users asu
               JOIN users u ON u.id = asu.user_id
               WHERE u.login = ? AND asu.id = ?"
        result (jdbc/execute-one! (:tx request) [query username (to-uuid auth-system-id)])]
    (:data result)))

;; Function to verify password
(defn verify-password [request username password auth-system-id]
  (if-let [hashed-password (fetch-hashed-password request username auth-system-id)]
    (try
      (println ">o> check password=" password "hashed-password=" hashed-password)
      ;; Use the `checkpw` function from bcrypt to verify the password
      (checkpw password hashed-password)
      (catch Exception e
        (println ">o> check exception=" e)
        false))
    false))

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

;; Handler to authenticate user
(defn authenticate-handler [request]
  (let [{:keys [username password auth-system-id]} (:body-params request)]
    (if (verify-password request username password auth-system-id)
      (response/response {:status "success" :message "User authenticated successfully"})
      (response/status (response/response {:status "failure" :message "Invalid credentials"}) 401))))

;; Function to update the hashed password in the database
(defn set-password [request username password auth-system-id]
  (let [
        ;hashed-password (hashers/derive password)  ;; Hash the plain password
        hashed-password (hashpw password)  ;; Hash the plain password
        query "UPDATE authentication_systems_users
               SET data = ?
               WHERE user_id = (SELECT id FROM users WHERE login = ?) AND id = ?"]
    (jdbc/execute-one! (:tx request) [query hashed-password username (to-uuid auth-system-id)])))

;; Handler for setting the user's password
(defn set-password-handler [request]
  (let [{:keys [username password auth-system-id]} (:body-params request)]
    (try
      (set-password request username password auth-system-id)
      (response/response {:status "success" :message "Password updated successfully"})
      (catch Exception e
        (println "Error updating password: " e)
        (response/status (response/response {:status "failure" :message "Error updating password"}) 500)))))

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

;; Define authentication routes
(defn token-routes []
  [["/"
    {:tags ["Login process"]}
    ["token/login"
     {:post {
             :description "Login with username and password. (admin / password)"
             :accept "application/json"
             :coercion reitit.coercion.schema/coercion
             :swagger {:security [{:basicAuth []}]}
             :handler basic-auth-handler
             :responses {200 {:description "OK" :body s/Any}
                         401 {:description "Unauthorized"}
                         500 {:description "Internal Server Error"}}}}]
    [["token/public" {:get hello-handler}]
     ["token/protected" {:get {
                         :description "Use 'Token &lt;token&gt;' as Authorization header."
                         :accept "application/json"
                         :coercion reitit.coercion.schema/coercion
                         :swagger {:security [{:BearerAuth []}]}
                         :handler protected-handler
                         :middleware [wrap-jwt-auth]}}]

     ;; Route to authenticate user
     ["auth/authenticate"
      {:post {
              :accept "application/json"
              :description "Authenticate user with username and password. (bcrypt)"
              :coercion reitit.coercion.schema/coercion
              :parameters {:body {:username s/Str
                                  :password s/Str
                                  :auth-system-id s/Uuid}}
              :handler authenticate-handler}}]

     ;; Route to set/update the password
     ["auth/set-password"
      {:post {
              :accept "application/json"
              :description "Set or update the user's password. (bcrypt)"
              :coercion reitit.coercion.schema/coercion
              :parameters {:body {:username s/Str
                                  :password s/Str
                                  :auth-system-id s/Uuid}}
              :handler set-password-handler}}]]]])
