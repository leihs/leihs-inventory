(ns leihs.inventory.server.resources.main
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   ;[leihs.core.sign-in.back :as be]
   [leihs.core.anti-csrf.back :as anti-csrf]
   [leihs.core.anti-csrf.back :refer [anti-csrf-token]]
   ;[leihs.inventory.server.constants :as consts]
   [leihs.core.sign-in.simple-login :refer [sign-in-view]]
   [leihs.inventory.server.utils.html-utils :refer [add-csrf-tags]]

   [leihs.core.sign-in.back :as be]
   [leihs.core.sign-in.simple-login :refer [sign-in-view]]
   [leihs.core.sign-out.back :as so]
   [leihs.core.status :as status]
   ;[leihs.inventory.server.constants :as consts :refer [HIDE_BASIC_ENDPOINTS]]
   [leihs.inventory.server.constants :as consts  :refer [APPLY_DEV_ENDPOINTS HIDE_BASIC_ENDPOINTS
                                             APPLY_ENDPOINTS_NOT_YET_USED_BY_FE]]

   [leihs.inventory.server.utils.helper :refer [convert-to-map snake-case-keys]]
   ;[leihs.core.anti-csrf.back :refer [anti-csrf-token]]
   [leihs.core.constants :as constants]
   ;[leihs.core.sign-in.back :as be]
   ;[leihs.core.sign-in.simple-login :refer [sign-in-view]]
   ;[leihs.core.sign-out.back :as so]
   ;[leihs.core.status :as status]
   ;[leihs.inventory.server.constants :as consts :refer [HIDE_BASIC_ENDPOINTS]]
   ;[leihs.inventory.server.constants :refer [APPLY_DEV_ENDPOINTS
   ;                                          APPLY_ENDPOINTS_NOT_YET_USED_BY_FE]]
   ;[leihs.inventory.server.resources.auth.auth-routes :refer [logout-handler session-token-routes]]
   ;[leihs.inventory.server.resources.auth.session :as ab]
   ;[leihs.inventory.server.resources.dev.routes :refer [get-dev-routes]]
   ;[leihs.inventory.server.resources.admin.status.routes :refer [get-admin-status-routes]]
   ;[leihs.inventory.server.resources.pool.buildings-rooms.routes :refer [get-buildings-rooms-routes]]
   ;[leihs.inventory.server.resources.pool.category-tree.routes :refer [get-category-tree-route]]
   ;[leihs.inventory.server.resources.pool.entitlement-groups.routes :refer [get-entitlement-groups-routes]]
   ;[leihs.inventory.server.resources.pool.export.routes :refer [get-export-routes]]
   ;[leihs.inventory.server.resources.pool.fields.routes :refer [get-fields-routes]]
   ;[leihs.inventory.server.resources.pool.items.routes :refer [get-items-routes]]
   ;[leihs.inventory.server.resources.pool.manufacturers.routes :refer [get-manufacturers-routes]]
   ;[leihs.inventory.server.resources.pool.models.model.attachments.attachment.routes :refer [get-models-model-attachments-single-routes]]
   ;[leihs.inventory.server.resources.pool.models.model.attachments.routes :refer [get-models-model-attachments-route]]
   ;[leihs.inventory.server.resources.pool.models.model.images.image.routes :refer [get-models-images-image-routes]]
   ;[leihs.inventory.server.resources.pool.models.model.images.image.thumbnail.routes :refer [get-models-images-single-thumbnail-routes]]
   ;[leihs.inventory.server.resources.pool.models.model.images.routes :refer [get-models-model-images-route]]
   ;[leihs.inventory.server.resources.pool.models.model.items.routes :refer [get-models-single-items-route]]
   ;[leihs.inventory.server.resources.pool.models.model.routes :refer [get-models-single-route]]
   ;[leihs.inventory.server.resources.pool.models.routes :refer [get-models-route]]
   ;[leihs.inventory.server.main :refer [post-sign-in get-sign-in post-sign-out swagger-api-docs-handler]]
   ;[leihs.inventory.server.resources.pool.responsible-inventory-pools.routes :refer [get-responsible-inventory-pools-routes]]
   ;[leihs.inventory.server.resources.pool.suppliers.routes :refer [get-suppliers-routes]]
   ;[leihs.inventory.server.resources.profile.routes :refer [get-profile-routes]]
   ;[leihs.inventory.server.resources.utils.middleware :refer [restrict-uri-middleware]]
   ;[leihs.inventory.server.utils.helper :refer [convert-to-map]]
   ;[leihs.inventory.server.utils.html-utils :refer [add-csrf-tags]]
   [muuntaja.core :as m]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [reitit.openapi :as openapi]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.swagger :as swagger]
   [ring.util.response :as response]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [debug info warn error]])
  (:gen-class))

;(thrown/reset-ns-filter-regex #"^(leihs|cider)\..*")


(defn swagger-api-docs-handler [request]
  (let [path (:uri request)]
    (cond
      (= path "/inventory/api-docs") (response/redirect "/inventory/api-docs/index.html")
      (= path "/inventory/index.html") (response/redirect "/inventory")
      :else (response/status (response/response "File not found") 404))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn convert-params [request]
  (if-let [form-params (:form-params request)]
    (let [converted-form-params (into {} (map (fn [[k v]] [(keyword k) v]) form-params))]
      (assoc request :form-params converted-form-params :form-params-raw converted-form-params))
    request))

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
