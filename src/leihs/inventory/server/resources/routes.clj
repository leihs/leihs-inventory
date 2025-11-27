(ns leihs.inventory.server.resources.routes
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [dev.routes :refer [get-dev-routes]]
   [leihs.inventory.server.constants :as consts :refer [APPLY_API_ENDPOINTS_NOT_USED_IN_FE
                                                        APPLY_DEV_ENDPOINTS
                                                        HIDE_BASIC_ENDPOINTS]]
   [leihs.inventory.server.middlewares.authorize :refer [wrap-authorize-for-pool wrap-authorize]]
   [leihs.inventory.server.resources.main :refer [get-csrf-token get-sign-in
                                                  get-sign-out post-sign-in
                                                  post-sign-out
                                                  swagger-api-docs-handler]]
   [leihs.inventory.server.resources.pool.buildings.building.routes :as building]
   [leihs.inventory.server.resources.pool.buildings.routes :as buildings]
   [leihs.inventory.server.resources.pool.category-tree.routes :as category-tree]
   [leihs.inventory.server.resources.pool.entitlement-groups.routes :as entitlement-groups]
   [leihs.inventory.server.resources.pool.export.csv.routes :as export-csv]
   [leihs.inventory.server.resources.pool.export.excel.routes :as export-excel]
   [leihs.inventory.server.resources.pool.fields.routes :as fields]
   [leihs.inventory.server.resources.pool.items.routes :as items]
   [leihs.inventory.server.resources.pool.list.routes :as list]
   [leihs.inventory.server.resources.pool.manufacturers.routes :as manufacturers]
   [leihs.inventory.server.resources.pool.models.model.attachments.attachment.routes :as attachment]
   [leihs.inventory.server.resources.pool.models.model.attachments.routes :as attachments]
   [leihs.inventory.server.resources.pool.models.model.images.image.routes :as image]
   [leihs.inventory.server.resources.pool.models.model.images.image.thumbnail.routes :as images-thumbnail]
   [leihs.inventory.server.resources.pool.models.model.images.routes :as images]
   [leihs.inventory.server.resources.pool.models.model.items.item.routes :as model-item]
   [leihs.inventory.server.resources.pool.models.model.items.routes :as model-items]
   [leihs.inventory.server.resources.pool.models.model.routes :as model]
   [leihs.inventory.server.resources.pool.models.routes :as models]
   [leihs.inventory.server.resources.pool.options.option.routes :as option]
   [leihs.inventory.server.resources.pool.options.routes :as options]
   [leihs.inventory.server.resources.pool.responsible-inventory-pools.routes :as responsible-inventory-pools]
   [leihs.inventory.server.resources.pool.rooms.room.routes :as room]
   [leihs.inventory.server.resources.pool.rooms.routes :as rooms]
   [leihs.inventory.server.resources.pool.software.routes :as software]
   [leihs.inventory.server.resources.pool.software.software.routes :as sw-software]
   [leihs.inventory.server.resources.pool.suppliers.routes :as suppliers]
   [leihs.inventory.server.resources.pool.templates.routes :as templates]
   [leihs.inventory.server.resources.pool.templates.template.routes :as template]
   [leihs.inventory.server.resources.profile.routes :as profile]


   [leihs.inventory.server.utils.middleware-handler :refer [wrap-html-40x
                                                            wrap-strict-format-negotiate
                                                            wrap-session-token-authenticate!]]

   ;[leihs.core.anti-csrf.back :as anti-csrf]
   ;[leihs.core.db :as db]
   ;[leihs.core.http-cache-buster2 :as cache-buster2]
   ;[leihs.core.ring-audits :as ring-audits]
   ;[leihs.core.routing.back :as core-routing]
   ;[leihs.core.routing.dispatch-content-type :as dispatch-content-type]
   ;;[leihs.core.settings :as settings]
   ;[leihs.inventory.server.resources.routes :as routes]
   ;[leihs.inventory.server.swagger :as swagger]
   ;[leihs.inventory.server.utils.coercion :refer [wrap-handle-coercion-error]]
   ;[leihs.inventory.server.utils.csrf-handler :as csrf]
   ;[leihs.inventory.server.utils.debug :as debug-mw]
   ;[leihs.inventory.server.utils.exception-handler :refer [wrap-exception]]
   ;[leihs.inventory.server.utils.middleware-handler :refer [wrap-html-40x
   ;                                                         wrap-strict-format-negotiate
   ;                                                         wrap-session-token-authenticate!]]
   ;[leihs.inventory.server.utils.ressource-handler :refer [custom-not-found-handler]]
   ;[muuntaja.core :as m]
   ;[reitit.coercion.schema]
   ;[reitit.coercion.spec]
   ;[reitit.dev.pretty :as pretty]
   ;[reitit.ring :as ring]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.multipart :as multipart]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   ;[reitit.swagger]
   ;[ring.middleware.content-type :refer [wrap-content-type]]
   ;[ring.middleware.cookies :refer [wrap-cookies]]
   ;[ring.middleware.default-charset :refer [wrap-default-charset]]
   ;[ring.middleware.params :refer [wrap-params]]


   [leihs.inventory.server.resources.session.protected.routes :as session-protected]
   [leihs.inventory.server.resources.session.public.routes :as session-public]
   [leihs.inventory.server.resources.settings.routes :as settings]
   [leihs.inventory.server.resources.status.routes :as admin-status]
   [leihs.inventory.server.resources.token.protected.routes :as token-protected]
   [leihs.inventory.server.resources.token.public.routes :as token-public]
   [leihs.inventory.server.resources.token.routes :as token]
   [leihs.inventory.server.utils.middleware :refer [restrict-uri-middleware]]
   [leihs.inventory.server.utils.middleware-handler :refer [endpoint-exists?]]
   [leihs.inventory.server.utils.request-utils :refer [authenticated?]]
   [leihs.inventory.server.utils.response-helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [reitit.openapi :as openapi]
   [reitit.swagger :as swagger]
   [ring.util.codec :as codec]
   [schema.core :as s]
   [taoensso.timbre :refer [debug error]]))

(defn- create-root-page [_]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (str "<html><body><head><link rel=\"stylesheet\" href=\"/inventory/assets/css/additional.css\">
       </head><div class='max-width'>
       <img src=\"/inventory/assets/zhdk-logo.svg\" alt=\"ZHdK Logo\" style=\"margin-bottom:4em\" />
       <h1>Overview _> go to <a href=\"/inventory\">go to /inventory<a/></h1>"
              (slurp (io/resource "md/info.html")) "</div></body></html>")})

(defn sign-in-out-endpoints []
  [[""
    {:no-doc HIDE_BASIC_ENDPOINTS
     :get {:accept "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"
           :swagger {:produces ["text/html"] :security []}
           :produces ["text/html"]
           :description "Root page"
           :handler (fn [request]
                      (debug "Processing root request...")
                      (create-root-page request))}}]

   ["sign-in"
    {:swagger {:tags ["Login / Logout"]}
     :no-doc HIDE_BASIC_ENDPOINTS

     :post {:accept "text/html"
            :description "Authenticate user by login (set cookie with token)\n- Expects 'user' and 'password'"
            :produces ["text/html"]
            :coercion reitit.coercion.schema/coercion
            :handler post-sign-in}

     :get {:summary "HTML | Get sign-in page"
           :accept "text/html"
           :swagger {:consumes ["text/html"]
                     :produces ["text/html"]}
           :produces ["text/html"]
           :middleware [(restrict-uri-middleware ["/sign-in"])]
           :handler get-sign-in}}]

   ["sign-out"
    {:swagger {:tags ["Login / Logout"]}
     :no-doc HIDE_BASIC_ENDPOINTS
     :post {:accept "text/html"
            :produces ["text/html"]
            :handler post-sign-out}
     :get {:accept "text/html"
           :produces ["application/json"]
           :summary "HTML | Get sign-out page"
           :handler get-sign-out}}]])

(defn csrf-endpoints []
  ["/"
   {:swagger {:tags ["CSRF"]}
    :no-doc HIDE_BASIC_ENDPOINTS}

   ["test-csrf"
    {:no-doc HIDE_BASIC_ENDPOINTS
     :get {:accept "application/json"
           :produces ["application/json"]
           :description "Access allowed without x-csrf-token"
           :handler (fn [_] {:status 200})}
     :post {:accept "application/json"
            :produces ["application/json"]
            :description "Access denied without x-csrf-token"
            :handler (fn [_] {:status 200})}
     :put {:accept "application/json"
           :produces ["application/json"]
           :description "Access denied without x-csrf-token"
           :handler (fn [_] {:status 200})}
     :patch {:accept "application/json"
             :produces ["application/json"]
             :description "Access denied without x-csrf-token"
             :handler (fn [_] {:status 200})}
     :delete {:accept "application/json"
              :produces ["application/json"]
              :description "Access denied without x-csrf-token"
              :handler (fn [_] {:status 200})}}]

   ["csrf-token/"
    {:no-doc false
     :get {:summary "Retrieves the X-CSRF-Token required for using non-GET Swagger endpoints."
           :description "Set token in Swagger UI by Authorize-Button -> Field: csrfToken"
           :accept "application/json"
           :swagger {:produces ["application/json"]}
           :produces ["application/json"]
           :handler get-csrf-token}}]])

(def mime-types
  {"html" "text/html"
   "htm" "text/html"
   "css" "text/css"
   "js" "application/javascript"
   "json" "application/json"
   "png" "image/png"
   "jpg" "image/jpeg"
   "jpeg" "image/jpeg"
   "gif" "image/gif"
   "svg" "image/svg+xml"
   "txt" "text/plain"})

(defn content-type [filename]
  (let [ext (-> filename
                (str/split #"\.")
                last
                str/lower-case)]
    (get mime-types ext "application/octet-stream")))

(defn html-endpoints []
  [""
   {:swagger {:tags ["Html"]}
    :no-doc HIDE_BASIC_ENDPOINTS}

   ["{*path}"
    {:no-doc HIDE_BASIC_ENDPOINTS
     :fallback? true
     :get {:description "Public assets like JS, CSS, images"
           :produces ["text/html"]
           :handler (fn [request]

                      (println ">o> inventory-defaults: " (:uri request))

                      (let [router (:reitit.router request)
                            method (:request-method request)
                            uri (:uri request)
                            route-data (endpoint-exists? router method uri)
                            exists? (boolean route-data)]
                        (if (authenticated? request)
                          (rh/index-html-response request)
                          (let [query-string (:query-string request)
                                full-url (if query-string
                                           (str uri "?" query-string)
                                           uri)
                                encoded-url (codec/url-encode full-url)
                                redirect-url (str "/sign-in?return-to=" encoded-url)]
                            {:status 302
                             :headers {"Location" redirect-url
                                       "Content-Type" "text/html"}
                             :body ""}))))}}]])

(defn settings-endpoint []
  ["/"
   {:swagger {:tags ["Settings"]}}
   (settings/routes)])

(defn swagger-endpoints []
  ["/"

   {:middleware [                  reitit.swagger/swagger-feature
                 ]}

   ["api-docs"
    {:get {:handler swagger-api-docs-handler
           :public true
           :produces ["text/html"]
           :no-doc true}}

    ["/swagger.json"
     {:get {:no-doc true
            :public true
            :produces ["text/html" "application/json"]
            :swagger {:info {:title "inventory-api"
                             :version "2.0.0"
                             :description (str (slurp (io/resource "md/info.html")) (slurp (io/resource "md/routes.html")))}
                      :securityDefinitions {:apiAuth {:type "apiKey" :name "Authorization" :in "header"}
                                            :csrfToken {:type "apiKey" :name "x-csrf-token" :in "header"}}
                      :security [{:csrfToken []}]}
            :handler (swagger/create-swagger-handler)}}]

    ["/openapi.json"
     {:get {:no-doc true
            :public true
            :produces ["text/html" "application/json"]
            :openapi {:openapi "3.0.0"
                      :info {:title "inventory-api"
                             :description (str (slurp (io/resource "md/info.html")) (slurp (io/resource "md/routes.html")))
                             :version "3.0.0"}}
            :handler (openapi/create-openapi-handler)}}]]

   ["swagger-ui{*path}"
    {:no-doc HIDE_BASIC_ENDPOINTS
     :get {:accept "text/html"
           :public true
           :swagger {:produces ["text/html"]}
           :description "Swagger-UI with filter/sort"
           :produces ["text/html"]
           :handler (fn [request]
                      (try
                        (let [file "public/swagger-ui/index.html"
                              content-type (content-type file)
                              resource (io/resource file)]
                          {:status 200 :headers {"Content-Type" content-type} :body (slurp resource)})
                        (catch Exception e
                          (error "Error processing swagger-ui request:" e)
                          (rh/index-html-response request))))}}]])

(defn visible-api-endpoints
  "Returns a vector of the core routes plus any additional routes passed in."
  []
  (let [core-routes [["/:pool_id" {:middleware [wrap-authorize-for-pool]
                                   :parameters {:path {:pool_id s/Uuid}}}
                      (models/routes)
                      (model/routes)
                      (list/routes)
                      (software/routes)
                      (sw-software/routes)
                      (option/routes)
                      (options/routes)

                      ["/models/:model_id/images"

                       {:middleware [#(wrap-html-40x % [#"/inventory/.+/images/.+"
                                                        #"/inventory/.+/images/.+/thumbnail"
                                                        #"/inventory/.+/attachments/.+"])]}

                      (image/routes)
                      (images/routes)
                      (images-thumbnail/routes)
                      ]

                      (attachment/routes)
                      (attachments/routes)
                      (items/routes)
                      (templates/routes)
                      (template/routes)
                      (model-items/routes)
                      (model-item/routes)
                      (building/routes)
                      (buildings/routes)
                      (room/routes)
                      (rooms/routes)
                      (category-tree/routes)
                      (entitlement-groups/routes)

                      (manufacturers/routes)
                      (responsible-inventory-pools/routes)
                      (suppliers/routes)

                      (when APPLY_API_ENDPOINTS_NOT_USED_IN_FE
                        [(suppliers/routes)
                         (fields/routes)
                         (export-csv/routes)
                         (export-excel/routes)
                         (fields/routes)
                         (items/routes)])

                      (when APPLY_DEV_ENDPOINTS
                        [(get-dev-routes)])]

                     (admin-status/routes)
                     (profile/routes)
                     (session-protected/routes)
                     (session-public/routes)
                     (token-protected/routes)
                     (token-public/routes)
                     (token/routes)]]
    (vec core-routes)))

(defn all-api-endpoints []
  ["/"
   (sign-in-out-endpoints)
   ["inventory"
    {:swagger {:tags [""]}
     :middleware [

                  parameters/parameters-middleware
                  muuntaja/format-negotiate-middleware
                  muuntaja/format-response-middleware

                  muuntaja/format-request-middleware
                  coercion/coerce-response-middleware
                  coercion/coerce-request-middleware
                  multipart/multipart-middleware

                  wrap-authorize
                  ]}
    (settings-endpoint)
    (swagger-endpoints)
    (csrf-endpoints)
    (visible-api-endpoints)
    (html-endpoints)]])
