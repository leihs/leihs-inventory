(ns leihs.inventory.server.routes
  (:refer-clojure :exclude
                  [keyword replace])
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [leihs.core.anti-csrf.back :refer [anti-csrf-props anti-csrf-token]]
   [leihs.core.auth.session :refer [wrap-authenticate]]
   [leihs.core.constants]
   [leihs.core.constants :as constants]
   [leihs.core.sign-in.back :as be]
   [leihs.core.sign-in.simple-login :refer [sign-in-view]]
   [leihs.core.sign-out.back :as so]
   [leihs.core.status :as status]
   [leihs.inventory.server.constants :as consts]
   [leihs.inventory.server.resources.attachments.routes :refer [get-attachments-routes]]
   [leihs.inventory.server.resources.auth.auth-routes :refer [authenticate-handler
                                                              logout-handler
                                                              set-password-handler
                                                              update-role-handler
                                                              token-routes]]
   [leihs.inventory.server.resources.auth.session :as ab]
   [leihs.inventory.server.resources.buildings_rooms.routes :refer [get-buildings-rooms-routes]]
   [leihs.inventory.server.resources.categories.routes :refer [get-categories-routes]]
   [leihs.inventory.server.resources.dev.routes :refer [get-dev-routes]]
   [leihs.inventory.server.resources.export.routes :refer [get-export-routes]]
   [leihs.inventory.server.resources.fields.routes :refer [get-fields-routes]]
   [leihs.inventory.server.resources.images.routes :refer [get-images-routes]]
   [leihs.inventory.server.resources.items.routes :refer [get-items-routes]]
   [leihs.inventory.server.resources.models.main]
   [leihs.inventory.server.resources.models.routes :refer [get-model-by-pool-route get-model-route]]
   [leihs.inventory.server.resources.models.tree.routes :refer [get-tree-route]]
   [leihs.inventory.server.resources.owner-department.routes :refer [get-owner-department-routes]]
   [leihs.inventory.server.resources.pools.routes :refer [get-pools-routes]]
   [leihs.inventory.server.resources.properties.routes :refer [get-properties-routes]]
   [leihs.inventory.server.resources.supplier.routes :refer [get-supplier-routes]]
   [leihs.inventory.server.resources.user.routes :refer [get-user-routes]]
   [leihs.inventory.server.utils.helper :refer [convert-to-map]]
   [leihs.inventory.server.utils.html-utils :refer [add-csrf-tags]]
   [muuntaja.core :as m]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [reitit.openapi :as openapi]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.swagger :as swagger]
   [ring.middleware.accept]
   [ring.middleware.accept]
   [ring.util.response :as response]
   [ring.util.response :refer [bad-request redirect response status]]
   [schema.core :as s]))

(defn swagger-api-docs-handler [request]
  (let [path (:uri request)]
    (cond
      (= path "/inventory/api-docs") (redirect "/inventory/api-docs/index.html")
      (= path "/inventory/index.html") (redirect "/inventory")
      :else {:status 404
             :body "File not found"})))

(defn convert-params [request]
  (if-let [form-params (:form-params request)]
    (let [converted-form-params (into {} (map (fn [[k v]] [(clojure.core/keyword k) v]) form-params))]
      (-> request
          (assoc :form-params converted-form-params)
          (assoc :form-params-raw converted-form-params)))
    request))

(defn- incl-other-routes []
  ["" (get-model-route)
   (get-model-by-pool-route)
   (get-tree-route)
   (get-properties-routes)
   (get-pools-routes)
   (get-categories-routes)
   (get-buildings-rooms-routes)
   (get-dev-routes)

   (get-owner-department-routes)
   (get-items-routes)
   (get-supplier-routes)
   (get-fields-routes)
   (get-export-routes)
   (get-attachments-routes)
   ;(get-accessories-routes)
   ;(get-entitlements-routes)
   ;(get-model-links-routes)
   (get-images-routes)
   (get-user-routes)
   (token-routes)])

(defn get-sign-in [request]
  (let [mtoken (anti-csrf-token request)
        query (convert-to-map (:query-params request))
        params (-> {:authFlow {:returnTo (or (:return-to query) "/inventory/models")}
                    :flashMessages []}
                   (assoc :csrfToken (when consts/ACTIVATE-SET-CSRF
                                       {:name "csrf-token" :value mtoken}))
                   (cond-> (not (nil? (:message query)))
                     (assoc :flashMessages [{:level "error" :messageID (:message query)}])))
        html (as-> (sign-in-view params) $
               (add-csrf-tags $ params))]
    {:status 200
     :headers {"Content-Type" "text/html; charset=utf-8"}
     :body html}))

(defn post-sign-in [request]
  (let [request-method (:request-method request)
        uri (:uri request)
        request (assoc request :settings {})
        form-data (get request :form-params)
        username (:user form-data)
        password (:password form-data)
        resp (if (or (str/blank? username) (str/blank? password))
               (be/create-error-response username request)
               (let [request (if consts/ACTIVATE-DEV-MODE-REDIRECT
                               (assoc-in request [:form-params :return-to] "/inventory/8bd16d45-056d-5590-bc7f-12849f034351/models")
                               request)
                     resp (be/routes (convert-params request))
                     created-session (get-in resp [:cookies "leihs-user-session" :value])
                     request (-> request
                                 (assoc :sessions created-session)
                                 (assoc-in [:cookies "leihs-user-session" :value] created-session))]

                 resp))]
    resp))

(defn get-sign-out [request]
  (let [uuid (get-in request [:cookies constants/ANTI_CSRF_TOKEN_COOKIE_NAME :value])
        params {:authFlow {:returnTo "/inventory/models"}
                :csrfToken (when consts/ACTIVATE-SET-CSRF
                             {:name "csrf-token" :value uuid})}
        html (as-> "public/dev-logout.html" $
               (io/resource $)
               (slurp $)
               (add-csrf-tags $ params))]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body html}))

(defn post-sign-out [request]
  (so/routes (convert-params request)))

(def update-role-response {:role-before s/Str
                           :role-after s/Str
                           :inventory_pool_id s/Uuid
                           :count-of-direct-access-right-should-be-one s/Int
                           (s/optional-key :update-result) s/Any})

(defn basic-routes []

  ["/"
   [[""
     ["sign-in"
      {:no-doc false
       :post {:accept "text/html"
              :swagger {:produces ["application/multipart-form-data"]}
              :handler post-sign-in}
       :get {:summary "HTML | Get sign-in page"
             :accept "text/html"
             :swagger {:produces ["text/html"]}

             :handler get-sign-in}}]

     ["sign-out"
      {:no-doc false
       :post {:accept "text/html"
              :middleware [wrap-authenticate]
              :handler post-sign-out}
       :get {:accept "text/html"
             :summary "HTML | Get sign-out page"
             :handler get-sign-out}}]]

    ["inventory"

     ["/"
      {:swagger {:tags ["Auth"] :security []}}

      ["login"
       {:get {:summary "[SIMPLE-LOGIN] OK | DEV | Authenticate user by login ( set cookie with token ) [v0]"
              :accept "application/json"
              :coercion reitit.coercion.schema/coercion
              :swagger {:security [{:basicAuth []}] :deprecated true}
              :handler authenticate-handler}}]

      ["admin/update-role"
       {:put {:summary "[] OK | DEV | Update direct-user-role [v0]"
              :accept "application/json"
              :description "- default pool-id: 8bd16d45-056d-5590-bc7f-12849f034351"
              :parameters {:query {:role (s/enum "inventory_manager" "lending_manager" "group_manager" "customer")
                                   (s/optional-key :pool_id) s/Uuid}}
              :coercion reitit.coercion.schema/coercion
              :middleware [wrap-authenticate]
              :handler update-role-handler
              :responses {200 {:description "OK"
                               :body update-role-response}
                          409 {:description "Conflict"
                               :body update-role-response}
                          500 {:description "Internal Server Error"}}}}]

      ["logout"
       {:get {:accept "application/json"
              :coercion reitit.coercion.schema/coercion
              :swagger {:security [] :deprecated true}
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

     (incl-other-routes)]]])

;#### debug ###################################################################
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
