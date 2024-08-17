(ns leihs.inventory.server.resources.models.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.models.main :as mn]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]

   [buddy.auth.backends :refer [jws]]
   [buddy.auth.middleware :refer [wrap-authentication]]
   [buddy.sign.jwt :as jwt]

   [reitit.ring :as ring]
   ;[myapp.auth :refer [wrap-jwt-auth]]


   [ring.middleware.accept]
   [schema.core :as s]))

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
  {;:id s/Uuid
   :type s/Str
   (s/optional-key :manufacturer) (s/maybe s/Str)
   :product s/Str
   ;(s/optional-key :version) (s/maybe s/Str)
   ;(s/optional-key :info_url) (s/maybe s/Str)
   ;(s/optional-key :rental_price) (s/maybe s/Num)
   ;(s/optional-key :maintenance_period) (s/maybe s/Int)
   ;(s/optional-key :is_package) (s/maybe s/Bool)
   ;(s/optional-key :hand_over_note) (s/maybe s/Str)
   ;(s/optional-key :description) (s/maybe s/Str)
   ;(s/optional-key :internal_description) (s/maybe s/Str)
   ;(s/optional-key :technical_detail) (s/maybe s/Str)
   ;:created_at s/Inst
   ;:updated_at s/Inst
   ;(s/optional-key :cover_image_id) (s/maybe s/Uuid)
   })

(defn accept-json-middleware [handler]
  (fn [request]
    (let [accept-header (get-in request [:headers "accept"])]
      (if (and accept-header (re-matches #"^.*application/json.*$" accept-header))
        (handler request)
        rh/INDEX-HTML-RESPONSE-OK))))

(defn get-model-route []
  [["/models"
   {:conflicting true
    :tags ["Models"]}

   [""
    {:get {:conflicting true
           :accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :middleware [accept-json-middleware]
           :swagger {:produces ["application/json" "text/html"]
                     ;:parameters {:query {:page {:description "Page number, defaults to 1"
                     ;                            :required false
                     ;                            :type "integer"
                     ;                            :default 1}
                     ;                     :size {:description "Number of items per page, defaults to 100"
                     ;                            :required false
                     ;                            :type "integer"
                     ;                            :default 100}
                     ;                     :sort_by {:description "Sort order"
                     ;                               :required false
                     ;                               :enum ["manufacturer-asc" "manufacturer-desc" "product-asc" "product-desc"]}
                     ;                     :filter_manufacturer {:description "Filter by manufacturer"
                     ;                                           :required false
                     ;                                           :type "string"}
                     ;                     :filter_product {:description "Filter by product"
                     ;                                      :required false
                     ;                                      :type "string"}}}
                     }

           ;; Actual parameters schema for query (Reitit Coercion)
           ;; params (get-in request [:parameters :query])
           :parameters {:query {
                                ;:id s/Uuid
                                :page s/Int
                                :size s/Int
                                :sort_by (s/enum :manufacturer-asc :manufacturer-desc :product-asc :product-desc)
                                (s/optional-key :filter_manufacturer) s/Str
                                (s/optional-key :filter_product) s/Str}}

           :handler mn/get-models-handler
           :responses {200 {:description "OK"
                            :body (s/->Either [s/Any schema])}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}

     :post {:summary "Create model."
            :accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :parameters {:body schema-min}
            :middleware [accept-json-middleware]
            :handler mn/create-model-handler
            :responses {200 {:description "Returns the created model."
                             :body s/Any}
                        400 {:description "Bad Request / Duplicate key value of ?product?"
                             :body s/Any}}}}]

   ["/:id"
    {:get {:accept "application/json"
           :conflicting true
           :coercion reitit.coercion.schema/coercion
           :middleware [accept-json-middleware]
           :handler mn/get-models-handler
           :parameters {:path {:id s/Uuid}}
           :responses {200 {:description "OK"
                            :body (s/->Either [s/Any schema])}
                       204 {:description "No Content"}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}

     :put {:accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :parameters {:path {:id s/Uuid}
                        :body schema-min}
           :middleware [accept-json-middleware]
           :handler mn/update-model-handler
           :responses {200 {:description "Returns the updated model."
                            :body s/Any}}}

     :delete {:accept "application/json"
              :coercion reitit.coercion.schema/coercion
              :parameters {:path {:id s/Uuid}}
              :middleware [accept-json-middleware]
              :handler mn/delete-model-handler
              :responses {200 {:description "Returns the deleted model."
                               :body s/Any}
                          400 {:description "Bad Request"
                               :body s/Any}}}}]]])

(defn get-model-by-pool-route []
  [["/:pool_id"
   {:conflicting true
    :tags ["Models by pool"]}

   [""
    {:get {:conflicting true
           :accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :middleware [accept-json-middleware]
           :swagger {:produces ["application/json" "text/html"]}
           :parameters {:path {:pool_id s/Uuid}}
           :handler mn/get-models-of-pool-handler
           :responses {200 {:description "OK"
                            :body (s/->Either [s/Any schema])}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]

   ["/models/:model_id"
    {:get {:accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :middleware [accept-json-middleware]
           :swagger {:produces ["application/json" "text/html"]}
           :parameters {:path {:pool_id s/Uuid
                               :model_id s/Uuid}}
           :handler mn/get-models-of-pool-handler
           :responses {200 {:description "OK"
                            :body (s/->Either [s/Any schema])}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]]])


;; Secret key for signing tokens
(def secret "mysecretkey")

;; Define JWT backend middleware
(def auth-backend
  (jws {:secret secret}))

(defn wrap-jwt-auth [handler]
  (wrap-authentication handler auth-backend))

(defn generate-token [user-id]
  (jwt/sign {:user-id user-id}
    secret
    {:alg :hs256}))


(defn hello-handler [request]
  {:status 200
   :body "Hello, World!"})

(defn protected-handler [request]
  {:status 200
   :body "This is a protected route!"})


(defn login-handler [request]
  (let [
        p (println ">o> auth1")

        {:keys [username password]} (:body-params request)

        ]
    ;; Validate username and password (this is just an example)
    (if (and (= username "admin")
          (= password "password"))
      {:status 200
       :body {:token (generate-token username)}}
      {:status 401
       :body "Invalid credentials"})))

(defn token-routes []
  [["/"
   {
    ;:conflicting true
    :tags ["Login process"]}

   ["login"
    {:post {
            ;:conflicting true
           :accept "application/json"
           :coercion reitit.coercion.schema/coercion
            :description "Login with username and password (admin/password)"
           ;:middleware [accept-json-middleware]
           ;:swagger {:produces ["application/json"]}
           :parameters {:body {
                               :username s/Str
                               :password s/Str
                               }}
           :handler login-handler
           :responses {200 {:description "OK"
                            ;:body (s/->Either [s/Any schema])
                            :body s/Any

                            }
                       401 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]

[["public" {:get hello-handler}]
 ["protected" {
               :get {
                     :security [{:BearerAuth []}]
                     :accept "application/json"
                     :coercion reitit.coercion.schema/coercion
                     :swagger {:security [{:BearerAuth []}]}

                     :handler protected-handler}
                :middleware [wrap-jwt-auth]}]

 ]


]])
