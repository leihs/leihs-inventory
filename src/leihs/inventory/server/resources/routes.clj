(ns leihs.inventory.server.resources.routes
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [hiccup.page :refer [html5]]
   [leihs.inventory.server.constants :as consts :refer [APPLY_API_ENDPOINTS_NOT_USED_IN_FE
                                                        HIDE_BASIC_ENDPOINTS]]
   [leihs.inventory.server.middlewares.authorize :refer [wrap-authorize-for-pool wrap-authorize]]
   [leihs.inventory.server.middlewares.uri-restrict :refer [restrict-uri-middleware]]
   [leihs.inventory.server.resources.main :refer [get-csrf-token get-sign-in
                                                  get-sign-out post-sign-in
                                                  post-sign-out
                                                  swagger-api-docs-handler]]
   [leihs.inventory.server.resources.pool.buildings.building.routes :as building]
   [leihs.inventory.server.resources.pool.buildings.routes :as buildings]
   [leihs.inventory.server.resources.pool.category-tree.routes :as category-tree]
   [leihs.inventory.server.resources.pool.entitlement-groups.routes :as entitlement-groups]
   [leihs.inventory.server.resources.pool.fields.routes :as fields]
   [leihs.inventory.server.resources.pool.inventory-pools.routes :as inventory-pools]
   [leihs.inventory.server.resources.pool.items.item.attachments.attachment.routes :as i-attachment]
   [leihs.inventory.server.resources.pool.items.item.attachments.routes :as i-attachments]
   [leihs.inventory.server.resources.pool.items.item.routes :as item]
   [leihs.inventory.server.resources.pool.items.routes :as items]
   [leihs.inventory.server.resources.pool.list.routes :as list]
   [leihs.inventory.server.resources.pool.manufacturers.routes :as manufacturers]
   [leihs.inventory.server.resources.pool.models.model.attachments.attachment.routes :as m-attachment]
   [leihs.inventory.server.resources.pool.models.model.attachments.routes :as m-attachments]
   [leihs.inventory.server.resources.pool.models.model.images.image.routes :as image]
   [leihs.inventory.server.resources.pool.models.model.images.image.thumbnail.routes :as images-thumbnail]
   [leihs.inventory.server.resources.pool.models.model.images.routes :as images]
   [leihs.inventory.server.resources.pool.models.model.routes :as model]
   [leihs.inventory.server.resources.pool.models.routes :as models]
   [leihs.inventory.server.resources.pool.options.option.routes :as option]
   [leihs.inventory.server.resources.pool.options.routes :as options]
   [leihs.inventory.server.resources.pool.rooms.room.routes :as room]
   [leihs.inventory.server.resources.pool.rooms.routes :as rooms]
   [leihs.inventory.server.resources.pool.software.routes :as software]
   [leihs.inventory.server.resources.pool.software.software.routes :as sw-software]
   [leihs.inventory.server.resources.pool.suppliers.routes :as suppliers]
   [leihs.inventory.server.resources.pool.templates.routes :as templates]
   [leihs.inventory.server.resources.pool.templates.template.routes :as template]
   [leihs.inventory.server.resources.profile.routes :as profile]
   [leihs.inventory.server.resources.session.protected.routes :as session-protected]
   [leihs.inventory.server.resources.session.public.routes :as session-public]
   [leihs.inventory.server.resources.settings.routes :as settings]
   [leihs.inventory.server.resources.status.routes :as admin-status]
   [leihs.inventory.server.resources.token.protected.routes :as token-protected]
   [leihs.inventory.server.resources.token.public.routes :as token-public]
   [leihs.inventory.server.resources.token.routes :as token]
   [leihs.inventory.server.utils.response :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [reitit.openapi :as openapi]
   [reitit.swagger :as swagger]
   [schema.core :as s]
   [taoensso.timbre :refer [debug error spy]]))

(defn- create-root-page [_]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (html5
          [:head
           [:link {:rel "stylesheet" :href "/inventory/assets/css/additional.css"}]]
          [:body
           [:div {:class "max-width"}
            [:img {:src "/inventory/assets/zhdk-logo.svg"
                   :alt "ZHdK Logo"
                   :style "margin-bottom:4em"}]
            [:h1 "Overview _> go to " [:a {:href "/inventory"} "go to /inventory"]]]])})

(defn sign-in-out-endpoints []
  [[""
    {:no-doc HIDE_BASIC_ENDPOINTS
     :get {:accept "text/html"
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

(defn settings-endpoint []
  ["/"
   {:swagger {:tags ["Settings"]}}
   (settings/routes)])

(defn swagger-endpoints []
  ["/"

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
                             :description (slurp (io/resource "md/csrf.html"))}
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
                             :description (slurp (io/resource "md/csrf.html"))
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
                          (rh/index-html-response request 406))))}}]])

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
                      (image/routes)
                      (images/routes)
                      (image/routes)
                      (images-thumbnail/routes)
                      (m-attachment/routes)
                      (m-attachments/routes)
                      (item/routes)
                      (items/routes)
                      (i-attachment/routes)
                      (i-attachments/routes)
                      (templates/routes)
                      (template/routes)
                      (building/routes)
                      (buildings/routes)
                      (room/routes)
                      (rooms/routes)
                      (category-tree/routes)
                      (entitlement-groups/routes)

                      (manufacturers/routes)
                      (inventory-pools/routes)
                      (suppliers/routes)

                      (when APPLY_API_ENDPOINTS_NOT_USED_IN_FE
                        [(suppliers/routes)
                         (fields/routes)
                         (fields/routes)])]

                     (admin-status/routes)
                     (profile/routes)
                     (session-protected/routes)
                     (session-public/routes)
                     (token-protected/routes)
                     (token-public/routes)
                     (token/routes)]]
    (vec core-routes)))

(def supported-accepts
  #{"text/html"
    "application/json"
    "image/"
    "text/csv"
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"})

(defn catch-all-handler [request]
  (let [authenticated? (-> request :authenticated-entity boolean)
        accept (str/lower-case (or (get-in request [:headers "accept"]) "*/*"))
        is-html? (or (str/includes? accept "text/html") (str/includes? accept "*/*"))
        is-image? (str/includes? accept "image/")
        supported? (or (= accept "*/*")
                       (some #(str/includes? accept %) supported-accepts))]
    (cond
      ;; Unsupported Accept header → 406
      (not supported?)
      {:status 406
       :headers {"content-type" "text/plain"}
       :body "Not Acceptable"}

      ;; HTML requests → SPA (client-side routing)
      is-html?
      (rh/index-html-response request 200)

      ;; Unauthenticated non-HTML → 401
      (not authenticated?)
      {:status 401
       :headers {"content-type" "application/json"}
       :body "{\"status\":\"failure\",\"message\":\"Not authenticated\"}"}

      ;; Authenticated image requests → 404 text/plain
      is-image?
      {:status 404
       :headers {"content-type" "text/plain"}
       :body "Not Found"}

      ;; Authenticated other formats → 404 JSON
      :else
      {:status 404
       :headers {"content-type" "application/json"}
       :body "{\"error\":\"Not Found\"}"})))

(defn all-api-endpoints []
  ["/"
   (sign-in-out-endpoints)
   ["inventory"
    {:swagger {:tags [""]}
     :middleware [wrap-authorize]}
    (settings-endpoint)
    (swagger-endpoints)
    (csrf-endpoints)
    (visible-api-endpoints)
    ;; Catch-all for unmatched /inventory/* routes
    ["*path"
     {:fallback? true
      :get {:handler catch-all-handler :no-doc true}
      :post {:handler catch-all-handler :no-doc true}
      :put {:handler catch-all-handler :no-doc true}
      :patch {:handler catch-all-handler :no-doc true}
      :delete {:handler catch-all-handler :no-doc true}}]]])
