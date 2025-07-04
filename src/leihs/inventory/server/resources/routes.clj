(ns leihs.inventory.server.resources.routes
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [leihs.core.anti-csrf.back :refer [anti-csrf-token]]
   [leihs.core.constants :as constants]
   [leihs.core.sign-in.back :as be]
   [leihs.core.sign-in.simple-login :refer [sign-in-view]]
   [leihs.core.sign-out.back :as so]
   [leihs.core.status :as status]
   [leihs.inventory.server.constants :as consts :refer [HIDE_BASIC_ENDPOINTS]]
   [leihs.inventory.server.constants :refer [APPLY_DEV_ENDPOINTS
                                             APPLY_ENDPOINTS_NOT_YET_USED_BY_FE]]
   [leihs.inventory.server.resources.auth.auth-routes :refer [logout-handler session-token-routes]]
   [leihs.inventory.server.resources.auth.session :as ab]
   [leihs.inventory.server.resources.dev.routes :refer [get-dev-routes]]
   [leihs.inventory.server.resources.admin.status.routes :refer [get-admin-status-routes]]
   [leihs.inventory.server.resources.pool.buildings-rooms.routes :refer [get-buildings-rooms-routes]]
   [leihs.inventory.server.resources.pool.category-tree.routes :refer [get-category-tree-route]]
   [leihs.inventory.server.resources.pool.entitlement-groups.routes :refer [get-entitlement-groups-routes]]
   [leihs.inventory.server.resources.pool.export.routes :refer [get-export-routes]]
   [leihs.inventory.server.resources.pool.fields.routes :refer [get-fields-routes]]
   [leihs.inventory.server.resources.pool.items.routes :refer [get-items-routes]]
   [leihs.inventory.server.resources.pool.manufacturers.routes :refer [get-manufacturers-routes]]
   [leihs.inventory.server.resources.pool.models.model.attachments.attachment.routes :refer [get-models-model-attachments-single-routes]]
   [leihs.inventory.server.resources.pool.models.model.attachments.routes :refer [get-models-model-attachments-route]]
   [leihs.inventory.server.resources.pool.models.model.images.image.routes :refer [get-models-images-image-routes]]
   [leihs.inventory.server.resources.pool.models.model.images.image.thumbnail.routes :refer [get-models-images-single-thumbnail-routes]]
   [leihs.inventory.server.resources.pool.models.model.images.routes :refer [get-models-model-images-route]]
   [leihs.inventory.server.resources.pool.models.model.items.routes :refer [get-models-single-items-route]]
   [leihs.inventory.server.resources.pool.models.model.routes :refer [get-models-single-route]]
   [leihs.inventory.server.resources.pool.models.routes :refer [get-models-route]]
   [leihs.inventory.server.resources.main :refer [post-sign-in get-sign-in post-sign-out get-sign-out swagger-api-docs-handler]]
   [leihs.inventory.server.resources.pool.responsible-inventory-pools.routes :refer [get-responsible-inventory-pools-routes]]
   [leihs.inventory.server.resources.pool.suppliers.routes :refer [get-suppliers-routes]]
   [leihs.inventory.server.resources.profile.routes :refer [get-profile-routes]]
   [leihs.inventory.server.resources.utils.middleware :refer [restrict-uri-middleware]]
   [leihs.inventory.server.utils.helper :refer [convert-to-map]]
   [leihs.inventory.server.utils.html-utils :refer [add-csrf-tags]]
   [muuntaja.core :as m]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [reitit.openapi :as openapi]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.swagger :as swagger]
   [ring.util.response :as response]
   [ring.util.response :refer [bad-request response status]]
   [schema.core :as s]))



; 1. Base routes (current state)
; 2. Already existing routes (not used by FE)
; 3. Dev routes
(defn incl-other-routes
  "Returns a vector of the core routes plus any additional routes passed in."
  []
  (let [core-routes [(get-models-route)
                     (get-models-single-route)
                     (get-models-images-image-routes)
                     (get-models-images-single-thumbnail-routes)
                     (get-models-model-attachments-route)
                     (get-models-model-attachments-single-routes)
                     (get-models-model-images-route)
                     (get-buildings-rooms-routes)
                     (get-models-single-items-route)
                     (get-responsible-inventory-pools-routes)
                     (get-entitlement-groups-routes)
                     (get-profile-routes)
                     (get-manufacturers-routes)
                     (get-category-tree-route)
                              (get-admin-status-routes)
                     (session-token-routes)]
        additional-routes (concat
                           (when APPLY_ENDPOINTS_NOT_YET_USED_BY_FE
                             [(get-suppliers-routes)
                              (get-fields-routes)
                              (get-export-routes)
                              (get-items-routes)])
                           (when APPLY_DEV_ENDPOINTS
                             [(get-dev-routes)]))]

    (vec (concat core-routes additional-routes))))


(defn pr [str fnc]
  ;(println ">oo> HELPER / " str fnc)(println ">oo> HELPER / " str fnc)
  (println ">oo> " str fnc)
  fnc)

(defn basic-routes []

  ["/"

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
            :handler get-sign-out}}]]

   ["inventory"

    ["/"
     {:swagger {:tags ["Auth"]}
      :no-doc HIDE_BASIC_ENDPOINTS}

    ;["/"
    ; {:swagger {:tags [""] :security [{:csrfToken []}]}}

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
                :handler (fn [_] {:status 200})}}]]

    ["/"
     {:swagger {:tags [""] :security []}}

     ["csrf-token/"
      {:no-doc false
       :get {:summary "Retrieve X-CSRF-Token to use swagger-endpoints"
             :accept "application/json"
             :swagger {:produces ["application/json"]}
             :handler get-sign-in}}]]

    ["/api-docs"
     {:get {:handler swagger-api-docs-handler
            :no-doc true}}]

    ["/api-docs/swagger.json"
     {:get {:no-doc true
            :swagger {:info {:title "inventory-api"
                             :version "2.0.0"
                             :description (str (slurp (io/resource "md/info.html")) (slurp (io/resource "md/routes.html")))}
                      :securityDefinitions {:apiAuth {:type "apiKey" :name "Authorization" :in "header"}
                                            :csrfToken {:type "apiKey" :name "x-csrf-token" :in "header"}}
                      :security [{:csrfToken []}]}
            :handler (swagger/create-swagger-handler)}}]
    ["/api-docs/openapi.json"
     {:get {:no-doc true
            :openapi {:openapi "3.0.0"
                      :info {:title "inventory-api"
                             :description (str (slurp (io/resource "md/info.html")) (slurp (io/resource "md/routes.html")))
                             :version "3.0.0"}}
            :handler (openapi/create-openapi-handler)}}]
    (incl-other-routes)]])
