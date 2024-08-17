(ns leihs.inventory.server.resources.models.routes
  (:require
   [buddy.auth :refer [authenticated?]]
   [buddy.auth.backends.token :refer [jws-backend]]
   [buddy.auth.middleware :refer [wrap-authentication]]
   [buddy.sign.jwt :as jwt]
   [clojure.set]
   [leihs.inventory.server.resources.models.main :as mn]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [schema.core :as s]))

;; Schemas for validation
(def schema
  {:id s/Uuid
   :type s/Str
   (s/optional-key :manufacturer) (s/maybe s/Str)
   :product s/Str
   (s/optional-key :version) (s/maybe s/Str)
   (s/optional-key :info_url) (s/maybe s/Str)
   (s/optional-key :rental_price) (s/maybe s/Num)
   (s/optional-key :maintenance_period) (s/maybe s/Int)
   (s/optional-key :is_package) (s/maybe s/Bool)
   (s/optional-key :hand_over_note) (s/maybe s/Str)
   (s/optional-key :description) (s/maybe s/Str)
   (s/optional-key :internal_description) (s/maybe s/Str)
   (s/optional-key :technical_detail) (s/maybe s/Str)
   :created_at s/Inst
   :updated_at s/Inst
   (s/optional-key :cover_image_id) (s/maybe s/Uuid)})

(def schema-min
  {:type s/Str
   (s/optional-key :manufacturer) (s/maybe s/Str)
   :product s/Str})

;; Helper middleware to ensure JSON responses
(defn accept-json-middleware [handler]
  (fn [request]
    (let [accept-header (get-in request [:headers "accept"])]
      (if (and accept-header (re-matches #"^.*application/json.*$" accept-header))
        (handler request)
        rh/INDEX-HTML-RESPONSE-OK))))

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

(defn login-handler [request]
  (let [{:keys [username password]} (:body-params request)]
    ;; Validate username and password (this is just an example)
    (if (and (= username "admin") (= password "password"))
      {:status 200 :body {:token (generate-token username)}}
      {:status 401 :body "Invalid credentials"})))

;; Define model routes (example)
(defn get-model-route []
  [["/models"
    {:tags ["Models"]}
    ["/"
     {:get {:accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :middleware [accept-json-middleware]
            :parameters {:query {:page s/Int
                                 :size s/Int
                                 :sort_by (s/enum :manufacturer-asc :manufacturer-desc :product-asc :product-desc)
                                 (s/optional-key :filter_manufacturer) s/Str
                                 (s/optional-key :filter_product) s/Str}}
            :handler mn/get-models-handler
            :responses {200 {:description "OK" :body (s/->Either [s/Any schema])}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}
      :post {:summary "Create model."
             :accept "application/json"
             :coercion reitit.coercion.schema/coercion
             :parameters {:body schema-min}
             :middleware [accept-json-middleware]
             :handler mn/create-model-handler
             :responses {200 {:description "Returns the created model." :body s/Any}
                         400 {:description "Bad Request" :body s/Any}}}}]]])

(defn get-model-by-pool-route []
  [["/:pool_id"
    {:tags ["Models by pool"]}
    ["/"
     {:get {:accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :middleware [accept-json-middleware]
            :parameters {:path {:pool_id s/Uuid}}
            :handler mn/get-models-of-pool-handler
            :responses {200 {:description "OK" :body (s/->Either [s/Any schema])}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]]])

;; Define authentication routes for token handling
(defn token-routes []
  [["/"
    {:tags ["Login process"]}
    ["login"
     {:post {
             :description "Login with username and password. (admin / password)"
             :accept "application/json"
             :coercion reitit.coercion.schema/coercion
             :parameters {:body {:username s/Str :password s/Str}}
             :handler login-handler
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

