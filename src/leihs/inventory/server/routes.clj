(ns leihs.inventory.server.routes
  (:refer-clojure :exclude [keyword replace])
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [leihs.core.status :as status]
   [leihs.inventory.server.resources.models.main]
   [leihs.inventory.server.resources.models.routes :refer [get-model-route get-model-by-pool-route]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.openapi :as openapi]
   [reitit.swagger :as swagger]
   [ring.util.mime-type :refer [ext-mime-type]]

   [ring.middleware.accept]
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
  (let [uri (:uri request)
        path (if (= "/inventory" uri) "index.html" uri)
        resource (or (io/resource (str "public/" path))
                     (io/resource (str "public/inventory" path)))]

    (cond
      (and (nil? resource) (= uri "/inventory/api-docs")) (redirect "/inventory/api-docs/index.html")
      resource {:status 200
                :body (slurp resource)}
      :else {:status 404
             :body "File not found"})))




;(defn inventory-handler [request]
;  (let [uri (:uri request)
;        p (println ">o> inventory-handler.uri=" uri)
;        path (if (= "/inventory" uri) "/index.html" uri)
;        p (println ">o> inventory-handler.path=" path)
;
;        resource (or
;                   ;(io/resource (str "public/" path))
;                   (io/resource (str "public/inventory" path))
;                   ;(io/resource (str "public/inventory/js" path))
;                   ;(io/resource (str "public/inventory/dist" path))
;                   (io/resource (str "public/inventory/assets" path))
;                   )
;
;        mime-type (ext-mime-type path)
;        p (println ">o> mime-type=" mime-type)
;        p (println ">o> resource=" resource)
;
;        ]
;
;    (println ">o> abc!!!!!!!!!!!!!!!!!!!!!!!")
;
;    (cond
;      (and (nil? resource) (= uri "/inventory/api-docs"))
;      {:status 302 :headers {"Location" "/inventory/api-docs/index.html"}}
;
;      resource
;      (do
;       (println ">o> abc1" resource)
;
;        {:status 200
;       :headers {"Content-Type" (or mime-type "application/octet-stream")}
;       :body (slurp resource)})
;
;      (and (nil? resource) (= path "index.html"))
;      (do
;       (println ">o> abc2")
;       {:status 200
;       :headers {"Content-Type" (or mime-type "application/octet-stream")}
;       :body (slurp resource)})
;
;      :else
;      {:status 404
;       :body "File not found"})))




(defn inventory-handler [request]
  (println ">o> inventory-handler" )
  (let [uri (:uri request)
        path (if (= "/inventory" uri) "/index.html" uri)
        resource (io/resource (str "public/inventory" path))
        mime-type (or (ext-mime-type path)
                    (cond
                      (.endsWith path ".css") "text/css"
                      (.endsWith path ".js") "application/javascript"
                      :else "application/octet-stream"))]
    (cond
      (and (nil? resource) (= uri "/inventory/api-docs")) {:status 302 :headers {"Location" "/inventory/api-docs/index.html"}}
      resource {:status 200 :headers {"Content-Type" mime-type} :body (slurp resource)}
      :else {:status 404 :body "File not found"})))



(defn- incl-other-routes []
  ;; TODO: add other routes here
  ["" (get-model-route) (get-model-by-pool-route)])

(defn basic-routes []
  [["/" {:no-doc true :get {:handler root-handler}}]

   ["/inventory"

    ;[""
    ; {:get {:handler inventory-handler :no-doc true}}]
    ;
    ;["/index.html"
    ; {:get {:handler inventory-handler :no-doc true}}]
    ;
    ;["/assets/index-z2lRr12x.css"
    ; {:get {:handler inventory-handler :no-doc true}}]
    ;
    ;["/assets/index-Dh2A7FpX.js"
    ; {:get {:handler inventory-handler :no-doc true}}]


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
    ;(incl-other-routes)

    ]])









;#### debug ###################################################################
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
