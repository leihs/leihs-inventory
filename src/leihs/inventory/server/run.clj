(ns leihs.inventory.server.run
  (:require
   [clojure.java.io :as io]
   [clojure.pprint :refer [pprint]]
   [clojure.string]
   [clojure.tools.cli :as cli]
   [leihs.core.db :as datasource]
   [leihs.core.db :as db]
   [leihs.core.http-server :as http-server]
   [leihs.core.shutdown :as shutdown]

   [leihs.core.status :as status]
   [leihs.core.url.jdbc]
   [leihs.inventory.server.resources.models.main :as mn]
   [leihs.inventory.server.routes :as routes]
   [leihs.inventory.server.swagger-api :as api]
   [logbug.catcher :as catcher]
   [muuntaja.core :as m]
   [reitit.coercion.schema]
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

   [schema.core :as s]
   [taoensso.timbre :refer [info]]))



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

(def schema-post
  {
   :id s/Uuid
   :type s/Str
   ;(s/optional-key :manufacturer)  s/Str
   ;:product s/Str
   ;(s/optional-key :version)  s/Str
   ;(s/optional-key :info_url)  s/Str
   ;(s/optional-key :rental_price)  s/Num
   ;(s/optional-key :maintenance_period)  s/Int
   ;(s/optional-key :is_package)  s/Bool
   ;(s/optional-key :hand_over_note)  s/Str
   ;(s/optional-key :description)  s/Str
   ;(s/optional-key :internal_description)  s/Str
   ;(s/optional-key :technical_detail)  s/Str
   ;:created_at s/Inst
   ;:updated_at s/Inst
   ;(s/optional-key :cover_image_id)  s/Uuid
   })








(defn create-app [options]
  (let [http-handler (routes/init options)
        router (ring/router

                 [["/" {:no-doc true :get {:handler api/root-handler}}]

                  ["/inventory"

                   [#"/(?!api-docs).*"
                    {:get {:handler api/inventory-handler}}]

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
                    {:get {:handler api/inventory-handler :no-doc true}}]

                   ["/js/*"
                    {:get {:handler api/inventory-handler :no-doc true}}]

                   ["/assets/*"
                    {:get {:handler api/inventory-handler :no-doc true}}]

                   ["/css/*"
                    {:get {:handler api/inventory-handler :no-doc true}}]

                   ["/models"
                    {:tags ["Models"]}

                    ["" {:get {:accept "application/json"
                               :coercion reitit.coercion.schema/coercion
                               :middleware [api/accept-json-middleware]
                               :handler mn/get-models-handler

                               :responses {200 {:description "OK"
                                                :body [schema]
                                                }
                                           404 {:description "Not Found"
                                                ;:content {:application/json {:schema {:type "string"}}}
                                                }
                                           500 {:description "Internal Server Error"
                                                ;:content {:application/json {:schema {:type "string"}}}
                                                }
                                           }
                               }

                         :post {
                                :accept "application/json"
                                :coercion reitit.coercion.schema/coercion

                                :parameters {:body schema-min}
                                :middleware [api/accept-json-middleware]
                                :handler mn/create-model-handler
                                }
                         }]


                    ["/:id" {:get {:accept "application/json"
                                   :coercion reitit.coercion.schema/coercion
                                   :middleware [api/accept-json-middleware]
                                   :handler mn/get-models-handler
                                   :parameters {:path {:id s/Uuid}}

                                   :responses {200 {:description "OK"
                                                    :body schema}
                                               404 {:description "Not Found"
                                                    ;:content {:application/json {:schema {:type "string"}}}
                                                    }
                                               500 {:description "Internal Server Error"
                                                    ;:content {:application/json {:schema {:type "string"}}}
                                                    }
                                               }
                                   }

                             :put {
                                   :accept "application/json"
                                   :coercion reitit.coercion.schema/coercion
                                   :parameters {:path {:id s/Uuid}
                                                :body schema-min}
                                   :middleware [api/accept-json-middleware]
                                   :handler mn/update-model-handler
                                   }

                             :delete {
                                      :accept "application/json"
                                      :coercion reitit.coercion.schema/coercion
                                      :parameters {:path {:id s/Uuid}}
                                      :middleware [api/accept-json-middleware]
                                      :handler mn/delete-model-handler
                                      }
                             }
                     ]

                    ]

                   ]]



                 {:exception pretty/exception

                  :data {
                         :coercion reitit.coercion.spec/coercion
                         :muuntaja m/instance
                         :middleware [
                                      datasource/wrap-tx

                                      swagger/swagger-feature
                                      parameters/parameters-middleware
                                      muuntaja/format-negotiate-middleware
                                      muuntaja/format-response-middleware
                                      exception/exception-middleware
                                      muuntaja/format-request-middleware
                                      coercion/coerce-response-middleware
                                      coercion/coerce-request-middleware
                                      multipart/multipart-middleware]}})]
    (ring/ring-handler router
      (ring/routes

        (swagger-ui/create-swagger-ui-handler
          {:path "/inventory/api-docs/"
           :config {:validatorUrl nil
                    :urls [
                           {:name "swagger" :url "swagger.json"}
                           {:name "openapi" :url "openapi.json"}]
                    :urls.primaryName "openapi"
                    :operationsSorter "alpha"}})
        (ring/create-default-handler)
        )

      )))

(defn run [options]
  (catcher/snatch
    {:return-fn (fn [e] (System/exit -1))}
    (info "Invoking run with options: " options)
    (shutdown/init options)
    (let [status (status/init)]
      (db/init options (:health-check-registry status)))
    (let [
          ;http-handler (routes/init options)
          ]

      ;(http-server/start options http-handler)
      ;(http-server/start options (api/app))
      (http-server/start options (create-app options))
      ;(jetty/run-jetty #'app {:port 4000, :join? false})

      ;(api/main)

      )))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def cli-options
  (concat
    [["-h" "--help"]
     shutdown/pid-file-option]
    (http-server/cli-options :default-http-port 3260)
    db/cli-options))

(defn main-usage [options-summary & more]
  (->> ["leihs-inventory"
        ""
        "usage: leihs-perm [<gopts>] run [<opts>] [<args>]"
        ""
        "Options:"
        options-summary
        ""
        ""
        (when more
          ["-------------------------------------------------------------------"
           (with-out-str (pprint more))
           "-------------------------------------------------------------------"])]
    flatten (clojure.string/join \newline)))

(defn main [gopts args]
  (let [{:keys [options arguments errors summary]}
        (cli/parse-opts args cli-options :in-order true)
        pass-on-args (->> [options (rest arguments)]
                       flatten (into []))
        options (merge gopts options)]
    (cond
      (:help options) (println (main-usage summary {:args args :options options}))
      :else (run options))))
