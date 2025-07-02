(ns leihs.inventory.server.routes
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
   [leihs.inventory.server.resources.auth.auth-routes :refer [authenticate-handler logout-handler session-token-routes
                                                              ;token-routes
                                                              ]]
   [leihs.inventory.server.resources.auth.session :as ab]
   [leihs.inventory.server.resources.dev.routes :refer [get-dev-routes]]
   ;[leihs.inventory.server.resources.pool-by-access-right.routes :refer [get-pool-by-access-right-routes]]
   [leihs.inventory.server.resources.pool.attachments.routes :refer [get-attachments-routes]]
   [leihs.inventory.server.resources.pool.buildings-rooms.routes :refer [get-buildings-rooms-routes]]
   ;[leihs.inventory.server.resources.pool.categories.category.routes :refer [get-categories-category-route]]

   [leihs.inventory.server.resources.pool.categories.routes :refer [get-categories-routes]]
   ;[leihs.inventory.server.resources.pool.categories.routes :refer [get-categories-routes]]
   [leihs.inventory.server.resources.pool.category-tree.routes :refer [get-category-tree-route]]

   ;[leihs.inventory.server.resources.pool.category-links.routes :refer [get-category-links-routes]]

   ;[leihs.inventory.server.resources.pool.departments.department.routes :refer [get-departments-single-routes]]
   ;[leihs.inventory.server.resources.pool.departments.routes :refer [get-departments-routes]]

   ;[leihs.inventory.server.resources.pool.entitlement-groups.entitlement-group.routes :refer [get-entitlement-groups-single-routes]]

   [leihs.inventory.server.resources.pool.entitlement-groups.routes :refer [get-entitlement-groups-routes]]
   [leihs.inventory.server.resources.pool.export.routes :refer [get-export-routes]]
   ;[leihs.inventory.server.resources.pool.fields.field.routes :refer [get-fields-single-routes]]
   [leihs.inventory.server.resources.pool.fields.routes :refer [get-fields-routes]]

   ;[leihs.inventory.server.resources.pool.groups.group.routes :refer [get-groups-single-routes]]
   ;[leihs.inventory.server.resources.pool.groups.routes :refer [get-groups-routes]]

   [leihs.inventory.server.resources.pool.images.image.routes :refer [get-images-image-routes]]

   [leihs.inventory.server.resources.pool.images.image.thumbnail.routes :refer [get-images-image-thumbnail-routes]]
   [leihs.inventory.server.resources.pool.images.routes :refer [get-images-routes]]

   [leihs.inventory.server.resources.pool.items.routes :refer [get-items-routes]]
   [leihs.inventory.server.resources.pool.manufacturers.routes :refer [get-manufacturers-routes]]

   [leihs.inventory.server.resources.pool.models-compatibles.routes :refer [get-models-compatibles-route]]

   ;[leihs.inventory.server.resources.pool.models.model.accessories.routes :refer [get-models-single-accessories-route]]
   [leihs.inventory.server.resources.pool.models.model.attachments.routes :refer [get-models-model-attachments-route]]
   ;[leihs.inventory.server.resources.pool.models.model.entitlements.routes :refer [get-models-single-entitlements-route]]
   [leihs.inventory.server.resources.pool.models.model.images.routes :refer [get-models-model-images-route]]
   [leihs.inventory.server.resources.pool.models.model.items.routes :refer [get-models-single-items-route]]

   [leihs.inventory.server.resources.pool.models.model.model-links.routes :refer [get-models-single-model-links-route]]
   ;[leihs.inventory.server.resources.pool.models.model.properties.routes :refer [get-models-single-properties-route]]

   [leihs.inventory.server.resources.pool.models.model.routes :refer [get-models-single-route]]
   [leihs.inventory.server.resources.pool.models.routes :refer [get-models-route]]
   ;[leihs.inventory.server.resources.pool.owners.owner.routes :refer [get-owners-single-routes]]
   ;[leihs.inventory.server.resources.pool.owners.routes :refer [get-owners-routes]]

   ;[leihs.inventory.server.resources.pool.properties.routes :refer [get-properties-routes]]
   ;[leihs.inventory.server.resources.pool.category-tree.routes :refer [get-tree-route]]
   ;[leihs.inventory.server.resources.pool.owner-department.routes :refer [get-owner-department-routes]]
   [leihs.inventory.server.resources.pool.responsible-inventory-pools.routes :refer [get-responsible-inventory-pools-routes]]
   [leihs.inventory.server.resources.pool.suppliers.routes :refer [get-suppliers-routes]]
   ;[leihs.inventory.server.resources.pool.suppliers.supplier.routes :refer [get-suppliers-single-routes]]
   [leihs.inventory.server.resources.profile.routes :refer [get-profile-routes]]
   ;[leihs.inventory.server.resources.pool.user.routes :refer [get-user-routes]]
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

(defn swagger-api-docs-handler [request]
  (let [path (:uri request)]
    (cond
      (= path "/inventory/api-docs") (response/redirect "/inventory/api-docs/index.html")
      (= path "/inventory/index.html") (response/redirect "/inventory")
      :else (response/status (response/response "File not found") 404))))

(defn convert-params [request]
  (if-let [form-params (:form-params request)]
    (let [converted-form-params (into {} (map (fn [[k v]] [(keyword k) v]) form-params))]
      (assoc request :form-params converted-form-params :form-params-raw converted-form-params))
    request))

; 1. Base routes (current state)
; 2. Already existing routes (not used by FE)
; 3. Dev routes
(defn incl-other-routes
  "Returns a vector of the core routes plus any additional routes passed in."
  []
  (let [core-routes [
                     ;(get-departments-routes)
                     ;(get-departments-single-routes)

                     ;(get-owners-routes)
                     ;(get-owners-single-routes)

                     (get-models-route)
                     (get-models-single-route)
                     (get-models-model-attachments-route)
                     (get-models-model-images-route)

                     (get-buildings-rooms-routes)
                     (get-models-single-items-route)
                     ;(get-models-single-entitlements-route)

                     ;(get-models-single-accessories-route)

                     (get-models-single-model-links-route)

;(get-tree-route)
                     (get-responsible-inventory-pools-routes)
                     ;(get-categories-routes)
                     (get-attachments-routes)

                     (get-entitlement-groups-routes)
                     ;(get-entitlement-groups-single-routes)

                     ;(get-groups-routes)
                     ;(get-groups-single-routes)

                     (get-profile-routes)

                     ;(get-models-single-properties-route)

                     (get-models-compatibles-route)
                     (get-manufacturers-routes)

                     ;(get-pool-by-access-right-routes)

                     ;(get-category-links-routes)
                     (get-category-tree-route)
                     (get-categories-routes)
                     ;(get-categories-category-route)

                     (get-images-routes)
                     (get-images-image-routes)
                     (get-images-image-thumbnail-routes)

                     (session-token-routes)]
        additional-routes (concat
                           (when APPLY_ENDPOINTS_NOT_YET_USED_BY_FE
                             [(get-suppliers-routes)
                              ;(get-suppliers-single-routes)

                              (get-fields-routes)
                              ;(get-fields-single-routes)
                              (get-export-routes)
                              (get-items-routes)
                              ;(get-properties-routes)
                               ])
                           (when APPLY_DEV_ENDPOINTS
                             [(get-dev-routes)]))]

    (vec (concat core-routes additional-routes))))

(defn get-sign-in [request]
  (let [mtoken (anti-csrf-token request)
        query (convert-to-map (:query-params request))
        params (-> {:authFlow {:returnTo (or (:return-to query) "/inventory/models")}
                    :flashMessages []}
                   (assoc :csrfToken (when consts/ACTIVATE-SET-CSRF
                                       {:name "csrf-token" :value mtoken}))
                   (cond-> (:message query)
                     (assoc :flashMessages [{:level "error" :messageID (:message query)}])))
        accept (get-in request [:headers "accept"])
        html (add-csrf-tags (sign-in-view params) params)]
    (if (str/includes? accept "application/json")
      {:status 200 :body params}
      {:status 200 :headers {"Content-Type" "text/html; charset=utf-8"} :body html})))

(defn post-sign-in [request]
  (let [form-data (:form-params request)
        username (:user form-data)
        password (:password form-data)
        csrf-token (:csrf-token form-data)]
    (if (or (str/blank? username) (str/blank? password))
      (be/create-error-response username request)
      (let [request (if consts/ACTIVATE-DEV-MODE-REDIRECT
                      (assoc-in request [:form-params :return-to] "/inventory/8bd16d45-056d-5590-bc7f-12849f034351/models")
                      request)
            resp (be/routes (convert-params request))
            created-session (get-in resp [:cookies "leihs-user-session" :value])
            request (assoc request :sessions created-session :cookies {"leihs-user-session" {:value created-session}})]
        resp))))

(defn get-sign-out [request]
  (let [uuid (get-in request [:cookies constants/ANTI_CSRF_TOKEN_COOKIE_NAME :value])
        params {:authFlow {:returnTo "/inventory/models"}
                :csrfToken (when consts/ACTIVATE-SET-CSRF
                             {:name "csrf-token" :value uuid})}
        html (add-csrf-tags (slurp (io/resource "public/dev-logout.html")) params)]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body html}))
(defn post-sign-out [request]
  (let [params (-> request
                   convert-params
                   (assoc-in [:accept :mime] :html))
        accept (get-in params [:headers "accept"])]
    (if (str/includes? accept "application/json")
      {:status (if (so/routes params) 200 409)}
      (so/routes params))))

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

     ["login"
      {:get {:summary "[SIMPLE-LOGIN] DEV | Authenticate user by login (set cookie with token) [fe]"
             :accept "application/json"
             :coercion reitit.coercion.schema/coercion
             :swagger {:security [{:basicAuth []} {:csrfToken []}] :deprecated true}
             :handler authenticate-handler}}]

     ;["admin/update-role"
     ; {:put {:summary "[] DEV | Update direct-user-role [fe]"
     ;        :accept "application/json"
     ;        :description "- default pool-id: 8bd16d45-056d-5590-bc7f-12849f034351"
     ;        :parameters {:query {:role (s/enum "inventory_manager" "lending_manager" "group_manager" "customer")
     ;                             (s/optional-key :pool_id) s/Uuid}}
     ;        :coercion reitit.coercion.schema/coercion
     ;        :middleware [wrap-is-admin!]
     ;        :handler update-role-handler
     ;        :responses {200 {:description "OK" :body update-role-response}
     ;                    409 {:description "Conflict" :body update-role-response}
     ;                    500 {:description "Internal Server Error"}}}}]

     ["logout"
      {:get {:accept "application/json"
             :coercion reitit.coercion.schema/coercion
             :swagger {:deprecated true}
             :middleware [ab/wrap]
             :handler logout-handler}}]

     ;["set-password"
     ; {:post {:summary "Set password by basicAuth for already authenticated user"
     ;         :accept "application/json"
     ;         :coercion reitit.coercion.schema/coercion
     ;         :parameters {:body {:new-password1 s/Str}}
     ;         :handler set-password-handler}}]
     ]
    ["/"
     {:swagger {:tags [] :security [{:csrfToken []}]}}

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
     {:swagger {:tags [] :security []}}

     ["csrf-token"
      {:no-doc false
       :get {:summary "Retrieve X-CSRF-Token for request header"
             :accept "application/json"
             :swagger {:produces ["application/json"]}
             :handler get-sign-in}}]]

    ;["/"
    ; {:swagger {:tags ["Dev"]}}
    ; ["admin/status"
    ;  {:get {:accept "application/json"
    ;         :handler status/status-handler
    ;         :middleware [wrap-is-admin!]}}]]

    ["/api-docs"
     {:get {:conflicting true
            :handler swagger-api-docs-handler
            :no-doc true}}]

    ["/api-docs/swagger.json"
     {:get {:no-doc true
            :swagger {:info {:title "inventory-api"
                             :version "2.0.0"
                             :description (str (slurp (io/resource "md/info.html")) (slurp (io/resource "md/routes.html")))}
                      :securityDefinitions {:apiAuth {:type "apiKey" :name "Authorization" :in "header"}
                                            :csrfToken {:type "apiKey" :name "x-csrf-token" :in "header"}
                                            :basicAuth {:type "basic"}}
                      :security [{:csrfToken []}]}
            :handler (swagger/create-swagger-handler)}}]
    ["/api-docs/openapi.json"
     {:get {:no-doc true
            :openapi {:openapi "3.0.0"
                      :info {:title "inventory-api"
                             :description (str (slurp (io/resource "md/info.html")) (slurp (io/resource "md/routes.html")))
                             :version "3.0.0"}}
            :handler (openapi/create-openapi-handler)}}]
    ;(all-routes)]])
    ;all-routes]])
    (incl-other-routes)]])
