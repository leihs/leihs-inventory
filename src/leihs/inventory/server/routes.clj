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

   [leihs.inventory.server.resources.auth.auth-routes :refer [authenticate-handler
                                                              logout-handler
                                                              set-password-handler
                                                              token-routes]]
   [leihs.inventory.server.resources.auth.session :as ab]
   [leihs.inventory.server.resources.categories.routes :refer [get-categories-routes]]
   [leihs.inventory.server.resources.fields.routes :refer [get-fields-routes]]

   [leihs.inventory.server.resources.images.routes :refer [get-images-routes]]
   [leihs.inventory.server.resources.items.routes :refer [get-items-routes]]
   [leihs.inventory.server.resources.models.main]
   [leihs.inventory.server.resources.models.routes :refer [get-model-by-pool-route get-model-route]]
   [leihs.inventory.server.resources.owner-department.routes :refer [get-owner-department-routes]]
   [leihs.inventory.server.resources.pools.routes :refer [get-pools-routes]]
   [leihs.inventory.server.resources.properties.routes :refer [get-properties-routes]]
   [leihs.inventory.server.resources.supplier.routes :refer [get-supplier-routes]]
   [leihs.inventory.server.resources.user.routes :refer [get-user-routes]]
   [leihs.inventory.server.utils.html-utils :refer [add-csrf-tags add-csrf-tags2]]
   [muuntaja.core :as m]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [reitit.openapi :as openapi]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.swagger :as swagger]
   [ring.middleware.accept]
   [ring.middleware.accept]
   ;[ring.util.response :refer [redirect]]
   [ring.util.response :refer [bad-request redirect response status]]
   [schema.core :as s]))

;(def   WHITELIST-URIS-FOR-API ["/sign-in"])

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
   (get-properties-routes)
   (get-pools-routes)
   (get-categories-routes)

   (get-owner-department-routes)
   (get-items-routes)
   (get-supplier-routes)
   (get-fields-routes)
   ;(get-attachments-routes)
   ;(get-accessories-routes)
   ;(get-entitlements-routes)
   ;(get-model-links-routes)

   (get-images-routes)
   (get-user-routes)
   (token-routes)])

(defn convert-to-map [dict]
  (into {} (map (fn [[k v]] [(clojure.core/keyword k) v]) dict)))

(defn get-sign-in [request]
  (let [mtoken (anti-csrf-token request)
        query (convert-to-map (:query-params request))

        ;; Construct params, including CSRF token if activated
        params (-> {:authFlow {:returnTo (or (:return-to query) "/inventory/models")}
                    :flashMessages []}
                   (assoc :csrfToken (when consts/ACTIVATE-SET-CSRF
                                       {:name "csrf-token" :value mtoken}))
                   (cond-> (not (nil? (:message query)))
                     (assoc :flashMessages [{:level "error" :messageID (:message query)}])))

        ;; Generate the original HTML and add CSRF tags
        html (as-> (sign-in-view params) $
               (add-csrf-tags $ params))]

    ;; Return the modified HTML in the response
    {:status 200
     :headers {"Content-Type" "text/html; charset=utf-8"}
     :body html}))

(defn post-sign-in [request]
  ;(defn handler [request]
  (let [request-method (:request-method request)
        uri (:uri request)

          ;; Initialize settings for the request
        request (assoc request :settings {})

          ;; Extract form data
        form-data (get request :form-params)
        username (:user form-data)
        password (:password form-data)

          ;; Validate form data
        resp (if (or (str/blank? username) (str/blank? password))
               (be/create-error-response username request)

               (let [;; Overrule redirect in dev mode if activated
                     request (if consts/ACTIVATE-DEV-MODE-REDIRECT
                               (assoc-in request [:form-params :return-to] "/inventory/8bd16d45-056d-5590-bc7f-12849f034351/models")
                               request)

                       ;; Process the sign-in request
                     resp (be/routes (convert-params request))

                       ;; Assign created session to the request
                     created-session (get-in resp [:cookies "leihs-user-session" :value])
                     request (-> request
                                 (assoc :sessions created-session)
                                 (assoc-in [:cookies "leihs-user-session" :value] created-session))]

                 resp))]

      ;; Log response for debugging
    (println ">o> Response:" resp)
    resp))

(defn get-sign-out [request]
  (let [uuid (get-in request [:cookies constants/ANTI_CSRF_TOKEN_COOKIE_NAME :value])
        params {:authFlow {:returnTo "/inventory/models"}
                :csrfToken (when consts/ACTIVATE-SET-CSRF
                             {:name "csrf-token" :value uuid})}

        ;; Load the HTML template and inject CSRF tokens if required
        html (as-> "public/dev-logout.html" $
               (io/resource $)
               (slurp $)
               (add-csrf-tags $ params))]

    {:status 200
     :headers {"Content-Type" "text/html"}
     :body html}))

(defn post-sign-out [request]
  (let [resp (so/routes (convert-params request))]
    ;; Log request method and URI for debugging purposes if needed.
    (println ">o> Handling sign-out request")
    (println ">o> Request Method:" (:request-method request))
    (println ">o> URI:" (:uri request))

    resp))

(defn basic-routes []

  ["/" [[;["/" {:no-doc false :get {:handler root-handler}}

         ""

         ["sign-in"
          {:no-doc false

           ;; TODO: how to fetch params from request?

           :post {:accept "text/html"
                  :swagger {:produces ["application/multipart-form-data"]}
                  :handler post-sign-in}

           :get {:summary "Get sign-in page"
                 :accept "text/html"
                 :swagger {:produces ["text/html"]}

                 :handler get-sign-in}}]

         ["sign-out"
          {:no-doc false

           :post {:accept "text/html"
                  :middleware [wrap-authenticate]
                  :handler post-sign-out}

           :get {:accept "text/html"
                 :handler get-sign-out}}]]

        ["inventory"

         ["/"
          {:swagger {:tags ["Auth"] :security []}}

          ["login"
           {:get {:summary "[] OK | Authenticate user by login ( set cookie with token )"
                  :accept "application/json"

                  :coercion reitit.coercion.schema/coercion
                  :swagger {:security [{:basicAuth []}] :deprecated true}
                  :handler authenticate-handler}}]

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

         ["/debug"
          {:tags ["Debug"]}]

         (incl-other-routes)]]])

;#### debug ###################################################################
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
