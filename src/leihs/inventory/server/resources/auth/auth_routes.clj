(ns leihs.inventory.server.resources.auth.auth-routes
  (:require
   [buddy.auth :refer [authenticated?]]
   [buddy.auth.backends.token :refer [jws-backend]]
   [buddy.auth.middleware :refer [wrap-authentication]]
   [buddy.sign.jwt :as jwt]
   [clojure.set]
   [clojure.string :as str]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [schema.core :as s])
  (:import (java.util Base64)))

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
                         :middleware [wrap-jwt-auth]}}]]]])

