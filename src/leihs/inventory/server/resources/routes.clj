(ns leihs.inventory.server.resources.routes
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [dev.routes :refer [get-dev-routes]]
   [leihs.core.constants :as constants]
   [leihs.core.sign-in.back :as be]
   [leihs.core.sign-out.back :as so]
   [leihs.core.status :as status]
   [leihs.inventory.server.constants :as consts :refer [APPLY_DEV_ENDPOINTS
                                                        APPLY_ENDPOINTS_NOT_YET_USED_BY_FE
                                                        HIDE_BASIC_ENDPOINTS]]
   [leihs.inventory.server.resources.admin.status.routes :as admin-status]
   [leihs.inventory.server.resources.main :refer [get-sign-in get-sign-out post-sign-in post-sign-out swagger-api-docs-handler]]
   [leihs.inventory.server.resources.pool.buildings.building.routes :as building]
   [leihs.inventory.server.resources.pool.buildings.routes :as buildings]
   [leihs.inventory.server.resources.pool.category-tree.routes :as category-tree]
   [leihs.inventory.server.resources.pool.entitlement-groups.routes :as entitlement-groups]
   [leihs.inventory.server.resources.pool.export.csv.routes :as export-csv]
   [leihs.inventory.server.resources.pool.export.excel.routes :as export-excel]
   [leihs.inventory.server.resources.pool.fields.routes :as fields]
   [leihs.inventory.server.resources.pool.items.routes :as items]
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
   [leihs.inventory.server.resources.pool.responsible-inventory-pools.routes :as responsible-inventory-pools]
   [leihs.inventory.server.resources.pool.rooms.room.routes :as room]
   [leihs.inventory.server.resources.pool.rooms.routes :as rooms]
   [leihs.inventory.server.resources.pool.suppliers.routes :as suppliers]
   [leihs.inventory.server.resources.profile.routes :as profile]
   [leihs.inventory.server.resources.session.protected.routes :as session-protected]
   [leihs.inventory.server.resources.session.public.routes :as session-public]
   [leihs.inventory.server.resources.token.protected.routes :as token-protected]
   [leihs.inventory.server.resources.token.public.routes :as token-public]
   [leihs.inventory.server.resources.token.routes :as token]
   [leihs.inventory.server.resources.utils.middleware :refer [restrict-uri-middleware]]
   [muuntaja.core :as m]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [reitit.openapi :as openapi]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.swagger :as swagger]
   [ring.util.response :as response]
   [ring.util.response :refer [bad-request response status]]
   [schema.core :as s]))

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
                     :produces ["text/html" "application/json"]}
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
   {:swagger {:tags ["Auth"]}
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
           :handler get-sign-in}}]])

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
  (let [core-routes [(models/routes)
                     (model/routes)
                     (image/routes)
                     (images/routes)
                     (images-thumbnail/routes)
                     (attachment/routes)
                     (attachments/routes)
                     (items/routes)
                     (model-items/routes)
                     (model-item/routes)
                     (admin-status/routes)
                     (building/routes)
                     (buildings/routes)
                     (room/routes)
                     (rooms/routes)
                     (category-tree/routes)
                     (entitlement-groups/routes)

                     (manufacturers/routes)
                     (responsible-inventory-pools/routes)
                     (suppliers/routes)

                     (profile/routes)
                     (session-protected/routes)
                     (session-public/routes)
                     (token-protected/routes)
                     (token-public/routes)
                     (token/routes)]
        additional-routes (concat
                           (when APPLY_ENDPOINTS_NOT_YET_USED_BY_FE
                             [(suppliers/routes)
                              (fields/routes)
                              (export-csv/routes)
                              (export-excel/routes)
                              (fields/routes)
                              (items/routes)])
                           (when APPLY_DEV_ENDPOINTS
                             [(get-dev-routes)]))]

    (vec (concat core-routes additional-routes))))

(defn all-api-endpoints []
  ["/"
   (sign-in-out-endpoints)
   ["inventory"
    (csrf-endpoints)
    (swagger-endpoints)
    (visible-api-endpoints)]])
