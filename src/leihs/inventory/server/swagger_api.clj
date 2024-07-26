(ns leihs.inventory.server.swagger-api
  (:require [clojure.java.io :as io]
            [clojure.string]
            [leihs.core.db :as db]
            [leihs.inventory.server.resources.models.main :as mn]
            [muuntaja.core :as m]
            [reitit.coercion.schema]
            [reitit.coercion.spec]
            [reitit.dev.pretty :as pretty]
            [reitit.openapi :as openapi]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.exception :as exception]
            [reitit.ring.middleware.multipart :as multipart]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [ring.middleware.resource :refer [wrap-resource]]
            [schema.core :as s]))

(defn root-handler [request]
  (let [accept-header (get-in request [:headers "accept"])]
    (cond
      (clojure.string/includes? accept-header "text/html")
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body (str "<html><body><h1>Welcome to my API _> go to <a href=\"/inventory\">go to /inventory<a/></h1></body></html>"
                  (slurp (io/resource "md/info.md")))}

      (clojure.string/includes? accept-header "application/json")
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body {:message "Welcome to my API"}}

      :else
      {:status 406
       :headers {"Content-Type" "text/plain"}
       :body "Not Acceptable1"})))

(defn inventory-handler [request]
  (let [path (:uri request)
        path (if (= "/inventory" path) "index.html" path)]
    (if-let [resource (or (io/resource (str "public/" path))
                        (io/resource (str "public/inventory/" path)))]
      {:status 200
       :body (slurp resource)}
      {:status 404
       :body "File not found"})))

(defn accept-json-middleware [handler]
  (fn [request]
    (let [accept-header (get-in request [:headers "accept"])]
      (if (and accept-header (re-matches #"^.*application/json.*$" accept-header))
        (handler request)
        {:status 404
         ;:headers {"Content-Type" "text/plain"}
         ;:body {:message "Not Acceptable: application/json required2"}
         }))))

(def schema
  {
   :id s/Uuid
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
   (s/optional-key :cover_image_id) (s/maybe s/Uuid)
   })

(def schema-min
  {
   ;:id s/Uuid
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

(defn create-app [options]
  (let [router (ring/router

                 [["/" {:no-doc true :get {:handler root-handler}}]

                  ["/inventory"

                   [#"/(?!api-docs).*"
                    {:get {:handler inventory-handler}}]

                   ["/api-docs/swagger.json"
                    {:get {:no-doc true
                           :swagger {:info {:title "inventory-api"
                                            :version "2.0.0"
                                            :description (slurp (io/resource "md/info.md"))
                                            }}
                           :handler (swagger/create-swagger-handler)}}]

                   ["/api-docs/openapi.json"
                    {:get {:no-doc true
                           :openapi {:openapi "3.0.0"
                                     :info {:title "inventory-api"
                                            :description (slurp (io/resource "md/info.md"))
                                            :version "3.0.0"}}
                           :handler (openapi/create-openapi-handler)}}]

                   [""
                    {:get {:handler inventory-handler :no-doc true}}]

                   ["/models"
                    {:tags ["Models"]}

                    ["" {:get {:accept "application/json"
                               :coercion reitit.coercion.schema/coercion
                               :middleware [accept-json-middleware]
                               :handler mn/get-models-handler

                               :responses {200 {:description "OK"
                                                :body [schema]
                                                }
                                           404 {:description "Not Found"}
                                           500 {:description "Internal Server Error"}
                                           }
                               }

                         :post {
                                :summary "Create model."
                                :accept "application/json"
                                :coercion reitit.coercion.schema/coercion

                                :parameters {:body schema-min}
                                :middleware [accept-json-middleware]
                                :handler mn/create-model-handler


                                :responses {200 {:description "Returns the workflows."
                                                 :body s/Any}
                                            400 {:description "Bad Reqeust / Duplicate key value of ?product?"
                                                 :body s/Any}}

                                }
                         }]


                    ["/:id" {:get {:accept "application/json"
                                   :coercion reitit.coercion.schema/coercion
                                   :middleware [accept-json-middleware]
                                   :handler mn/get-models-handler
                                   :parameters {:path {:id s/Uuid}}

                                   :responses {200 {:description "OK"
                                                    :body schema}
                                               204 {:description "No Content"}
                                               404 {:description "Not Found"}
                                               500 {:description "Internal Server Error"}
                                               }
                                   }

                             :put {
                                   :accept "application/json"
                                   :coercion reitit.coercion.schema/coercion
                                   :parameters {:path {:id s/Uuid}
                                                :body schema-min}
                                   :middleware [accept-json-middleware]
                                   :handler mn/update-model-handler

                                   :responses {200 {:description "Returns the updated model."
                                                    :body s/Any}
                                               }
                                   }

                             :delete {
                                      :accept "application/json"
                                      :coercion reitit.coercion.schema/coercion
                                      :parameters {:path {:id s/Uuid}}
                                      :middleware [accept-json-middleware]
                                      :handler mn/delete-model-handler

                                      :responses {200 {:description "Returns the workflows."
                                                       :body s/Any}
                                                  400 {:description "Bad Reqeust / Duplicate key value of ?product?"
                                                       :body s/Any}}
                                      }
                             }
                     ]]]]

                 {:exception pretty/exception
                  :data {
                         :coercion reitit.coercion.spec/coercion
                         :muuntaja m/instance
                         :middleware [db/wrap-tx

                                      swagger/swagger-feature
                                      parameters/parameters-middleware
                                      muuntaja/format-negotiate-middleware
                                      muuntaja/format-response-middleware
                                      exception/exception-middleware
                                      muuntaja/format-request-middleware
                                      coercion/coerce-response-middleware
                                      coercion/coerce-request-middleware
                                      multipart/multipart-middleware]}})]

    (-> (ring/ring-handler
          router
          (ring/routes
            (ring/redirect-trailing-slash-handler {:method :strip})

            ;; TODO: breaks swagger-ui
            ;(ring/create-default-handler {:not-found (constantly {:status 404 :body "LEIHS-DEV: Not Found"})})

            (swagger-ui/create-swagger-ui-handler
              {:path "/inventory/api-docs/"
               :config {:validatorUrl nil
                        :urls [{:name "swagger" :url "swagger.json"}
                               {:name "openapi" :url "openapi.json"}]
                        :urls.primaryName "openapi"
                        :operationsSorter "alpha"}})))
        ;(wrap-defaults site-defaults)
        (wrap-resource "public")
        )))