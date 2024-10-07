(ns leihs.inventory.server.routes
  (:refer-clojure :exclude
                  [keyword replace])
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [leihs.core.status :as status]
   [leihs.inventory.server.resources.accessories.routes :refer [get-accessories-routes]]
   [leihs.inventory.server.resources.attachments.routes :refer [get-attachments-routes]]
   [leihs.inventory.server.resources.auth.auth-routes :refer [authenticate-handler
                                                              logout-handler
                                                              set-password-handler
                                                              token-routes]]
   [leihs.inventory.server.resources.auth.session :as ab]
   [leihs.inventory.server.resources.categories.routes :refer [get-categories-routes]]
   [leihs.inventory.server.resources.entitlements.routes :refer [get-entitlements-routes]]
   [leihs.inventory.server.resources.images.routes :refer [get-images-routes]]
   [leihs.inventory.server.resources.items.routes :refer [get-items-routes]]
   [leihs.inventory.server.resources.model-links.routes :refer [get-model-links-routes]]
   [leihs.inventory.server.resources.models.main]
   [leihs.inventory.server.resources.models.routes :refer [get-model-by-pool-route get-model-route]]
   [leihs.inventory.server.resources.pools.routes :refer [get-pools-routes]]
   [leihs.inventory.server.resources.properties.routes :refer [get-properties-routes]]
   [leihs.inventory.server.resources.user.routes :refer [get-user-routes]]
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

(defn swagger-api-docs-handler [request]
  (let [path (:uri request)]
    (cond
      (= path "/inventory/api-docs") (redirect "/inventory/api-docs/index.html")
      (= path "/inventory/index.html") (redirect "/inventory")
      :else {:status 404
             :body "File not found"})))

(defn- incl-other-routes []
  ["" (get-model-route)
   (get-model-by-pool-route)
   (get-properties-routes)
   (get-pools-routes)

   (get-categories-routes)
   ;(get-items-routes)
   ;(get-attachments-routes)
   ;(get-accessories-routes)
   ;(get-entitlements-routes)
   ;(get-model-links-routes)

   (get-images-routes)
   (get-user-routes)
   (token-routes)])

(defn basic-routes []
  [["/" {:no-doc true :get {:handler root-handler}}]

   ["/inventory"

    ["/"
     {:swagger {:tags ["Auth"] :security []}}

     ["login"
      {:get {:summary "[] OK | Authenticate user by login ( set cookie with token )"
             :accept "application/json"
             :coercion reitit.coercion.schema/coercion
             :swagger {:security [{:basicAuth []}]}
             :handler authenticate-handler}}]

     ["logout"
      {:get {:accept "application/json"
             :coercion reitit.coercion.schema/coercion
             :swagger {:security []}
             :middleware [ab/wrap]
             :handler logout-handler}}]

     ["set-password"
      {:post {:summary "OK | Set password by basicAuth for already authenticated user"
              :accept "application/json"
              :coercion reitit.coercion.schema/coercion
              :swagger {:security [{:basicAuth []}]}
              :parameters {:body {:new-password1 s/Str}}
              :handler set-password-handler}}]]

    ["/"
     {:swagger {:tags ["Status"] :security []}}
     ["status"
      {:get {:accept "application/json"
             :handler status/status-handler
             :swagger {:security []}}}]]

    ["/api-docs"
     {:get {:conflicting true
            :handler swagger-api-docs-handler
            :no-doc true}}]

    ["/api-docs/swagger.json"
     {:get {:no-doc true
            :swagger {:info {:title "inventory-api"
                             :version "2.0.0"
                             :description (str (slurp (io/resource "md/info.html")) (slurp (io/resource "md/routes.html")))}

                      :securityDefinitions {:apiAuth {:type "apiKey"
                                                      :name "Authorization"
                                                      :in "header"}
                                            :basicAuth {:type "basic"}}
                      :security [{:basicAuth [] "auth" []}
                                 {:apiAuth {:type "apiKey"
                                            :name "Authorization"
                                            :in "header"}}]}
            :handler (swagger/create-swagger-handler)}}]

    ["/api-docs/openapi.json"
     {:get {:no-doc true
            :openapi {:openapi "3.0.0"
                      :info {:title "inventory-api"
                             :description (str (slurp (io/resource "md/info.html")) (slurp (io/resource "md/routes.html")))
                             :version "3.0.0"}}
            :handler (openapi/create-openapi-handler)}}]

    ["/debug"
     {:tags ["Debug"]}]

    (incl-other-routes)]])

;#### debug ###################################################################
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
