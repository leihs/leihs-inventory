(ns leihs.inventory.server.routes
  (:refer-clojure :exclude [keyword replace])
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [leihs.inventory.server.resources.models.main]
   [leihs.inventory.server.resources.models.routes :refer [get-model-route]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.openapi :as openapi]
   [reitit.swagger :as swagger]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn root-handler [request]
  (let [accept-header (get-in request [:headers "accept"])]
    (cond
      (clojure.string/includes? accept-header "text/html")
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body (str "<html><body><head><link rel=\"stylesheet\" href=\"/inventory/css/additional.css\">
       </head><div class='max-width'>
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
  (let [path (:uri request)
        path (if (= "/inventory" path) "index.html" path)]
    (if-let [resource (or (io/resource (str "public/" path))
                          (io/resource (str "public/inventory/" path)))]
      {:status 200
       :body (slurp resource)}
      {:status 404
       :body "File not found"})))

(defn- incl-other-routes []
  ;; TODO: add other routes here
  ;(concat get-model-route basic-routes)
  (get-model-route))

(defn basic-routes []
  [["/" {:no-doc true :get {:handler root-handler}}]

   ["/inventory"

    [#"/(?!api-docs).*"
     {:get {:handler inventory-handler}}]

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

    [""
     {:get {:handler inventory-handler :no-doc true}}]

    ["/debug"
     {:tags ["Debug"]}

     ["" {:get {:accept "text/html"
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
