(ns leihs.inventory.server.routes
  (:refer-clojure :exclude [keyword replace])
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [leihs.core.status :as status]
   [leihs.inventory.server.resources.models.main]
   [leihs.inventory.server.resources.models.routes :refer [get-model-by-pool-route get-model-route]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.openapi :as openapi]
   [reitit.swagger :as swagger]
   [ring.middleware.accept]
   [ring.util.response]
   [ring.util.response :refer [redirect]]
   [schema.core :as s]))

(defn root-handler [request]
  (let [accept-header (get-in request [:headers "accept"])]
    (cond
      (clojure.string/includes? accept-header "text/html")
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body (str "<html><body><head><link rel=\"stylesheet\" href=\"/inventory/css/additional.css\">
       </head><div class='max-width'>
       <img src=\"/inventory/static/zhdk-logo.svg\" alt=\"ZHdK Logo\" style=\"margin-bottom:4em\" />
       <h1>Overview _> go to <a href=\"/inventory\">go to /inventory<a/></h1>"
                  (slurp (io/resource "md/info.html")) "</div></body></html>")}

      (clojure.string/includes? accept-header "application/json")
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body (json/generate-string {:message "Welcome to Inventory-API"})}

      :else
      {:status 406
       :headers {"Content-Type" "text/plain"}
       :body "Not Acceptable"})))

(defn inventory-handler [request]
  ;(println ">o> inventory-handler")
  (let [path (:uri request)
        resource (or (io/resource (str "public/" path))
                     (io/resource (str "public/inventory" path)))]

    (cond
      (and (nil? resource) (= path "/inventory/api-docs")) (redirect "/inventory/api-docs/index.html")
      (= path "/inventory/index.html") (redirect "/inventory")
      resource {:status 200
                :body (slurp resource)}
      :else {:status 404
             :body "File not found"})))

(defn- incl-other-routes []
  ;; TODO: add other routes here
  ["" (get-model-route)
   (get-model-by-pool-route)])

(defn basic-routes []
  [["/" {:no-doc true :get {:handler root-handler}}]

   ["/inventory"

    ;; this works
    ["/models/inventory-list"
     {:get {:conflicting true
            :accept "text/html"
            :coercion reitit.coercion.schema/coercion
            :swagger {:produces ["text/html"]}
            :handler (fn [request] rh/INDEX-HTML-RESPONSE-OK)
            :responses {200 {:description "OK"
                             :body s/Any}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]

    ["/status"
     {:get {:accept "application/json"
            :handler status/status-handler}}]

    ["/api-docs"
     {:get {:conflicting true
            :handler inventory-handler :no-doc true}}]

    ["/api-docs/swagger.json"
     {:get {:no-doc true
            :swagger {:info {:title "inventory-api"
                             :version "2.0.0"
                             :description (str (slurp (io/resource "md/info.html")))}}
            :handler (swagger/create-swagger-handler)}}]

    ["/api-docs/openapi.json"
     {:get {:no-doc true
            :openapi {:openapi "3.0.0"
                      :info {:title "inventory-api"
                             :description (str (slurp (io/resource "md/info.html")))
                             :version "3.0.0"}}
            :handler (openapi/create-openapi-handler)}}]

    ["/index.html"
     {:get {:handler inventory-handler :no-doc true}}]

    ["/debug"
     {:tags ["Debug"]}

     ["" {:conflicting true
          :no-doc true
          :get {:accept "text/html"
                :coercion reitit.coercion.schema/coercion
                :swagger {:produces ["text/html"]}
                :handler (fn [request] rh/INDEX-HTML-RESPONSE-OK)
                :responses {200 {:description "OK"
                                 :body s/Any}
                            404 {:description "Not Found"}
                            500 {:description "Internal Server Error"}}}}]]
    (incl-other-routes)]])

;#### debug ###################################################################
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
