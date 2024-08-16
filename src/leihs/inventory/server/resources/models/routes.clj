(ns leihs.inventory.server.resources.models.routes
  (:require
   [buddy.auth :refer [authenticated?]]
   [buddy.auth.backends.token :refer [jws-backend]]
   [buddy.auth.middleware :refer [wrap-authentication]]
   [buddy.sign.jwt :as jwt]
   [clojure.java.io :as io]

   [leihs.inventory.server.resources.auth.auth-routes :as auth]

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
    (if (and (= username "admin") (= password "password"))
      {:status 200 :body {:token (generate-token username)}}
      {:status 401 :body "Invalid credentials"})))




(defn get-model-route []
  [["/models"
    {:tags ["Models"]}
    ["/"
     {:get {:accept "application/json"
            :summary "OK | Fetch models by pool with pagination/sort/filter"
            :description (slurp (io/resource "md/pagination-sort-filter.html"))
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



  [
   ;""
   ;{:conflicting true :tags ["/inventory"]}

  [
   ""
   {:conflicting true :tags ["inventory/* (by session OR token)"]}

   ["/dev/pools"
    {:get {:conflicting true
           :summary "OK | Fetch pools by session"
           :accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :middleware [accept-json-middleware]

           :parameters {
                        ;:path {:pool_id s/Uuid}
                        :query {
                                ;:page s/Int
                                ;:size s/Int
                                ;:sort_by (s/enum :manufacturer-asc :manufacturer-desc :product-asc :product-desc)
                                ;(s/optional-key :filter_manufacturer) s/Str
                                (s/optional-key :login) s/Str
                                (s/optional-key :firstname) s/Str
                                (s/optional-key :lastname) s/Str
                                }
                        }

           :swagger {:produces ["application/json"]
                     :deprecated true

                     :security [{
                                 ;:basicAuth []
                                 :SessionAuth []}]
                     }


           :handler mn/query-inventory-groups-by-login
           :responses {200 {:description "OK"
                            :body (s/->Either [s/Any schema])}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]

   ["/dev/current-user"
    {:get {:conflicting true
           :summary "OK | Fetch current-id by session"
           :accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :middleware [accept-json-middleware]

           :parameters {
                        ;:path {:pool_id s/Uuid}
                        :query {
                                ;:page s/Int
                                ;:size s/Int
                                ;:sort_by (s/enum :manufacturer-asc :manufacturer-desc :product-asc :product-desc)
                                ;(s/optional-key :filter_manufacturer) s/Str
                                (s/optional-key :login) s/Str
                                (s/optional-key :firstname) s/Str
                                (s/optional-key :lastname) s/Str
                                }
                        }

           :swagger {:produces ["application/json"]
                     :deprecated true
                     }
           :handler mn/query-user-handler-by-login
           :responses {200 {:description "OK"
                            :body (s/->Either [s/Any schema])}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]]







   [
    ""
    {:conflicting true :tags ["inventory/* (by session OR token)"]}

    ["/pools"
     {:get {:conflicting true
            :summary "OK | Fetch pools by session"
            :accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :middleware [accept-json-middleware
                         auth/wrap-authenticate-by-session
                         ]

            ;:parameters {
            ;             ;:path {:pool_id s/Uuid}
            ;             :query {
            ;                     ;:page s/Int
            ;                     ;:size s/Int
            ;                     ;:sort_by (s/enum :manufacturer-asc :manufacturer-desc :product-asc :product-desc)
            ;                     ;(s/optional-key :filter_manufacturer) s/Str
            ;                     (s/optional-key :login) s/Str
            ;                     (s/optional-key :firstname) s/Str
            ;                     (s/optional-key :lastname) s/Str
            ;                     }
            ;             }

            :swagger {:produces ["application/json"]
                      :security [{
                                  ;:basicAuth []
                                  :SessionAuth []}]}

            :handler mn/query-inventory-groups
            :responses {200 {:description "OK"
                             :body (s/->Either [s/Any schema])}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]

    ["/current-user"
     {:get {:conflicting true
            :summary "OK | Fetch current-id by session"
            :accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :middleware [accept-json-middleware
                         auth/wrap-authenticate-by-session
                         ]

            ;:parameters {
            ;             ;:path {:pool_id s/Uuid}
            ;             :query {
            ;                     ;:page s/Int
            ;                     ;:size s/Int
            ;                     ;:sort_by (s/enum :manufacturer-asc :manufacturer-desc :product-asc :product-desc)
            ;                     ;(s/optional-key :filter_manufacturer) s/Str
            ;                     (s/optional-key :login) s/Str
            ;                     (s/optional-key :firstname) s/Str
            ;                     (s/optional-key :lastname) s/Str
            ;                     }
            ;             }

            :swagger {:produces ["application/json"]
                      }
            :handler mn/query-user-handler
            :responses {200 {:description "OK"
                             :body (s/->Either [s/Any schema])}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]]







   [
    ""
    {:conflicting true :tags ["inventory/{pool-id}/models"]}
    [
    "/:pool_id"
    [""
     {:get {:conflicting true
            :summary "TODO"
            :description (slurp (io/resource "md/pagination-sort-filter.html"))
            :accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :middleware [accept-json-middleware]
            :swagger {:produces ["application/json" "text/html"]}
            :parameters {:path {:pool_id s/Uuid}}
            :handler mn/get-models-of-pool-handler
            :responses {200 {:description "OK"
                             ;:body (s/->Either [s/Any schema])}
                             :body s/Any}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]

    ["/models"
     {:get {:accept "application/json"
            :summary "OK"
            :coercion reitit.coercion.schema/coercion
            :middleware [accept-json-middleware]
            :swagger {:produces ["application/json" "text/html"]}
            :parameters {:path {:pool_id s/Uuid}
                         :query {
                                 ;:page s/Int
                                 ;:size s/Int
                                 ;:sort_by (s/enum :manufacturer-asc :manufacturer-desc :product-asc :product-desc)
                                 ;(s/optional-key :filter_manufacturer) s/Str
                                 (s/optional-key :model_id) s/Uuid}
                         }
            :handler mn/get-models-of-pool-handler
            :responses {200 {:description "OK"
                             ;:body (s/->Either [s/Any schema])}
                             :body s/Any}

                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]

    ["/models/:model_id"
     {:get {:accept "application/json"
            :summary "OK"
            :coercion reitit.coercion.schema/coercion
            :middleware [accept-json-middleware]
            :swagger {:produces ["application/json" "text/html"]}
            :parameters {:path {:pool_id s/Uuid
                                :model_id s/Uuid}}
            :handler mn/get-models-of-pool-handler
            :responses {200 {:description "OK"
                             ;:body (s/->Either [s/Any schema])}
                             :body s/Any}

                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]



    ["/model-groups"
     {:get {:accept "application/json"
            :summary "OK"
            :coercion reitit.coercion.schema/coercion
            :middleware [accept-json-middleware]
            :swagger {:produces ["application/json" "text/html"]}
            :parameters {:path {:pool_id s/Uuid}

                         :query {(s/optional-key :model_group_id) s/Uuid}
                         }
            :handler mn/get-model_groups-of-pool-handler
            :responses {200 {:description "OK"
                             ;:body (s/->Either [s/Any schema])}
                             :body s/Any}

                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]


    ]]


   ]

  )
