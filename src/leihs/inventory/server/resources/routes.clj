(ns leihs.inventory.server.resources.routes
  (:require
   [clojure.java.io :as io]
   [dev.routes :refer [get-dev-routes]]
   [leihs.inventory.server.constants :as consts :refer [APPLY_API_ENDPOINTS_NOT_USED_IN_FE
                                                        APPLY_DEV_ENDPOINTS
                                                        HIDE_BASIC_ENDPOINTS]]
   [leihs.inventory.server.resources.main :refer [get-csrf-token get-sign-in
                                                  get-sign-out post-sign-in
                                                  post-sign-out
                                                  swagger-api-docs-handler]]

   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [leihs.inventory.server.utils.response_helper :as rh]

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
   [leihs.inventory.server.resources.profile.routes :as profile]
   [leihs.inventory.server.resources.session.protected.routes :as session-protected]
   [leihs.inventory.server.resources.session.public.routes :as session-public]
   [leihs.inventory.server.resources.status.routes :as admin-status]
   [leihs.inventory.server.resources.token.protected.routes :as token-protected]
   [leihs.inventory.server.resources.token.public.routes :as token-public]
   [leihs.inventory.server.resources.token.routes :as token]
   [leihs.inventory.server.utils.middleware :refer [restrict-uri-middleware]]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [reitit.openapi :as openapi]
   [reitit.swagger :as swagger]))

(defn sign-in-out-endpoints []
  [["sign-in"
    {:swagger {:tags ["Login"]}
     :no-doc HIDE_BASIC_ENDPOINTS

     :post {:accept "application/json"
            :description "Authenticate user by login (set cookie with token)\n- Expects 'user' and 'password'"
            :swagger {:produces ["application/multipart-form-data"]}
            :coercion reitit.coercion.schema/coercion
            :handler post-sign-in}

     :get {:summary "HTML | Get sign-in page"
           :accept "text/html"
           :swagger {:consumes ["text/html"]
                     :produces ["text/html"]}
           :middleware [(restrict-uri-middleware ["/sign-in"])]
           :handler get-sign-in}}]

   ["sign-out"
    {:swagger {:tags ["Login"]}
     :no-doc HIDE_BASIC_ENDPOINTS
     :post {:accept "application/json"
            :swagger {:produces ["text/html" "application/json"]}
            :handler post-sign-out}
     :get {:accept "text/html"
           :summary "HTML | Get sign-out page"
           :handler get-sign-out}}]])

(defn csrf-endpoints []
  ["/"
   {:swagger {:tags ["CSRF"]}
    :no-doc HIDE_BASIC_ENDPOINTS}

   ["test-csrf"
    {:no-doc HIDE_BASIC_ENDPOINTS
     :get {:accept "application/json"
           :description "Access allowed without x-csrf-token"
           :handler (fn [_] {:status 200})}
     :post {:accept "application/json"
            :description "Access denied without x-csrf-token"
            :handler (fn [_] {:status 200})}
     :put {:accept "application/json"
           :description "Access denied without x-csrf-token"
           :handler (fn [_] {:status 200})}
     :patch {:accept "application/json"
             :description "Access denied without x-csrf-token"
             :handler (fn [_] {:status 200})}
     :delete {:accept "application/json"
              :description "Access denied without x-csrf-token"
              :handler (fn [_] {:status 200})}}]

   ["csrf-token/"
    {:no-doc false
     :get {:summary "Retrieves the X-CSRF-Token required for using non-GET Swagger endpoints."
           :description "Set token in Swagger UI by Authorize-Button -> Field: csrfToken"
           :accept "application/json"
           :swagger {:produces ["application/json"]}
           :handler get-csrf-token}}]])


(defn fetch-file-entry [uri assets]
  (if
   ;(and (file-request? uri) (clojure.string/includes? uri "/inventory/assets/"))
    (clojure.string/includes? uri "/inventory/assets/")
    (some (fn [[key value]]
            (if (str/includes? uri (str key))
              value))
      assets)
    nil))

(defn extract-filename [uri]
  (let [filename (last (str/split uri #"/"))]
    (if (and (not (empty? filename)) (re-matches #".*\.(css|js)$" filename))
      filename
      nil)))

(def mime-types
  {"html" "text/html"
   "htm"  "text/html"
   "css"  "text/css"
   "js"   "application/javascript"
   "json" "application/json"
   "png"  "image/png"
   "jpg"  "image/jpeg"
   "jpeg" "image/jpeg"
   "gif"  "image/gif"
   "svg"  "image/svg+xml"
   "txt"  "text/plain"})

(defn content-type [filename]
  (let [ext (-> filename
              (str/split #"\.")
              last
              str/lower-case)]
    (get mime-types ext "application/octet-stream")))

(defn csrf-endpoints []
  ["/"
   {:swagger {:tags ["Assets"]}
    :no-doc HIDE_BASIC_ENDPOINTS}

   ["assets/{*path}"
    {:no-doc HIDE_BASIC_ENDPOINTS
     :get {
           :accept "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7\n"
           :swagger {:produces ["application/json" "text/html" "image/png" "image/jpeg" "image/gif" "image/webp" "image/svg+xml"]}
           :description "Access allowed without x-csrf-token"

           :handler (fn [request]
(println ">o> abc.assets -> ASSET-HANDDLER!!!!!!!" )

                      (let [
                            ;request ((db/wrap-tx (fn [request] request)) request)
                            ;request ((csrf/extract-header (fn [request] request)) request)
                            ;request ((session/wrap-authenticate (fn [request] request)) request)
                            ;request ((dm/extract-dev-cookie-params (fn [request] request)) request)
                            uri (:uri request)
                            file (extract-filename uri)

                            content-type (content-type (or file ""))


                            ;assets (get-assets)
                            ;asset (fetch-file-entry uri assets)
                            ;accept-header (or (get-in request [:headers "accept"]) "")
                            ;referer (or (get-in request [:headers "referer"]) "")
                            ;swagger-call? (str/ends-with? (or referer "") "/inventory/api-docs/index.html")
                            ;accept-html? (clojure.string/includes? accept-header "text/html")
                            ]

                        (cond
                          ;(= uri "/") (create-root-page)

                          (and (str/starts-with? uri "/inventory/assets/locales/") (str/ends-with? uri "/translation.json")
                            ;(contains-one-of? uri CONST_SUPPORTED_LOCALES)
                            )
                          (let [src (str/replace-first uri "/inventory" "public/inventory")
                                resource (try (slurp (io/resource src))
                                              (catch Exception _ nil))]
                            (if resource
                              {:status 200 :headers {"Content-Type" "application/json"} :body resource}
                              {:status 404 :headers {"Content-Type" "application/json"}}))


                          :else  (try (let [
                                            ;{:keys [file content-type]} asset
                                      resource (io/resource file)


                                            ]
                                  ;(if resource

                                    ;(when resource
                                      {:status 200 :headers {"Content-Type" content-type} :body (slurp resource)}
                                        ;)

                                        )

                                      (catch Exception e
                                        (println ">o> abc.assets -> EXCEPTION!!!!!!!" e)
                                    (rh/index-html-response request 404)))

                                      ;))


                          ))




                      ;{:status 200}
                       )
           }

     }]
])

(defn swagger-endpoints []
  ["/api-docs"
   {:get {:handler swagger-api-docs-handler
          :no-doc true}}

   ["/swagger.json"
    {:get {:no-doc true
           :swagger {:info {:title "inventory-api"
                            :version "2.0.0"
                            :description (str (slurp (io/resource "md/info.html")) (slurp (io/resource "md/routes.html")))}
                     :securityDefinitions {:apiAuth {:type "apiKey" :name "Authorization" :in "header"}
                                           :csrfToken {:type "apiKey" :name "x-csrf-token" :in "header"}}
                     :security [{:csrfToken []}]}
           :handler (swagger/create-swagger-handler)}}]

   ["/openapi.json"
    {:get {:no-doc true
           :openapi {:openapi "3.0.0"
                     :info {:title "inventory-api"
                            :description (str (slurp (io/resource "md/info.html")) (slurp (io/resource "md/routes.html")))
                            :version "3.0.0"}}
           :handler (openapi/create-openapi-handler)}}]])

(defn visible-api-endpoints
  "Returns a vector of the core routes plus any additional routes passed in."
  []
  (let [core-routes [["/:pool_id"
                      (models/routes)
                      (list/routes)
                      (model/routes)
                      (software/routes)
                      (sw-software/routes)
                      (option/routes)
                      (options/routes)
                      (image/routes)
                      (images/routes)
                      (images-thumbnail/routes)
                      (attachment/routes)
                      (attachments/routes)
                      (items/routes)
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
    {:swagger {:tags [""]}}




    (csrf-endpoints)
    (swagger-endpoints)
    (visible-api-endpoints)]])
