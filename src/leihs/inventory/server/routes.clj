(ns leihs.inventory.server.routes
  (:refer-clojure :exclude
   [keyword replace])
  (:require
   [cheshire.core :as json]
   [clojure.string :as str]
   [clojure.java.io :as io]
   [leihs.core.anti-csrf.back :refer [anti-csrf-props anti-csrf-token]]
   [leihs.core.auth.session :refer [wrap-authenticate]]
   [leihs.core.constants :as constants]
   [leihs.core.constants]
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
   ;[leihs.core.sign-out.front :refer [component] :rename {component logout-component}]
   ;[leihs.core.sign-out.simple-logout :refer [sign-out-view]]

   [leihs.inventory.server.resources.items.routes :refer [get-items-routes]]
   ;[hiccup.page :refer [html5]]
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
  (let [accept-header (get-in request [:headers "accept"])

        ;p (println ">o> root-handler" )
        ;
        ;
        ;WHITELIST-URIS-FOR-API ["/sign-in"]
        ;uri (:uri request)
        ;p (println ">o> che??1" uri WHITELIST-URIS-FOR-API)
        ;
        ;p (println ">o> che??" (some #(= % uri) WHITELIST-URIS-FOR-API))
        ;
        ;p (println ">o> root-handler")
        ]
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
  (println ">o> convert-params" request)
  (if (nil? (:form-params request))
    (do
      (println ">o> convert-params / (:form-params request) NOT SET => " (:form-params request))
      request)

    (let [
          p (println ">o> convert-params.IN" (:form-params request))

          converted-form-params (into {} (map (fn [[k v]] [(clojure.core/keyword k) v]) (:form-params request)))

          p (println ">o> convert-params.OUT to [:form-params :form-params-raw]" converted-form-params)




          request (assoc request :form-params converted-form-params)
          request (assoc request :form-params-raw converted-form-params)

          ]
      ;(assoc request :form-params-raw converted-form-params)
      request
      )

    )
  )




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
  (into {} (map (fn [[k v]] [(clojure.core/keyword k) v]) dict))
  )
(defn get-sign-in [request]
  (let [
        mtoken (anti-csrf-token request)
        p (println ">o> (html) anti-csrf-token" mtoken)

        ;mprops (anti-csrf-props request)
        ;p (println ">o> (html) anti-csrf-props" mprops)

        uuid mtoken

        ;p (println ">o> abcA1" (:query-params request))

        p (println ">o> query1" (:query-params request))
        query (convert-to-map (:query-params request))
        p (println ">o> query2" query)
        ;query nil

        ;p (println ">o> abcA1a" query)
        p (println ">o> abcA2" (:query-params-raw request))


        ;uuid (str (UUID/randomUUID)) ;; Generate UUID for CSRF token
        params {:authFlow {:returnTo (or  (:return-to query) "/inventory/models")
                ;:csrfToken {:name "x-csrf-token" ;; should be csrf-token => back

                           }
                           ;:flashMessages [{:level "error" :message (:message query)}]
                           :flashMessages []


                :csrfToken {:name "csrf-token"
                            :value uuid}} ;; Parameters including CSRF token

        ; error-message (:message query)
        error-message (:message query)
        p (println ">o> 6paramsA error-message" error-message)
        p (println ">o> 6paramsB" params)

        ;;params (when (some? error-message)
        ;params (when (not (nil? error-message))
        ;    (assoc params :flashMessages [{:level "error" :messageID error-message}])
        ;    )

        params (cond (nil? error-message) params
                     (empty? error-message) params
                     :else (assoc params :flashMessages [{:level "error" :messageID error-message}])
                     )


        p (println ">o> 6paramsC" params)



        html (sign-in-view params) ;; Generate the original HTML using the params

        ;; Debugging the original HTML
        _ (println ">o> html.before" html (type html))

        ;; Add CSRF tokens to the HTML and debug the result
        html-with-csrf (add-csrf-tags html params)
        _ (println ">o> html.after" html-with-csrf (type html-with-csrf))
        ]

    ;; Return the modified HTML in the response
    {:status 200
     :headers {"Content-Type" "text/html; charset=utf-8"}
     :body html-with-csrf}))



;(defn get-sign-in [request]
;           (let [
;                 mtoken (anti-csrf-token request)
;                 p (println ">o> (html) anti-csrf-token" mtoken)
;
;                 mprops (anti-csrf-props request)
;                 p (println ">o> (html) anti-csrf-props" mprops)
;
;                 uuid mtoken
;
;
;                 ;uuid (str (UUID/randomUUID)) ;; Generate UUID for CSRF token
;                 params {:authFlow {:returnTo "/inventory/models"}
;                         ;:csrfToken {:name "x-csrf-token" ;; should be csrf-token => back
;                         :csrfToken {:name "csrf-token"
;                                     :value uuid}} ;; Parameters including CSRF token
;
;                 html (sign-in-view params) ;; Generate the original HTML using the params
;
;                 ;; Debugging the original HTML
;                 _ (println ">o> html.before" html (type html))
;
;                 ;; Add CSRF tokens to the HTML and debug the result
;                 html-with-csrf (add-csrf-tags html params)
;                 _ (println ">o> html.after" html-with-csrf (type html-with-csrf))
;                 ]
;
;             ;; Return the modified HTML in the response
;             {:status 200
;              :headers {"Content-Type" "text/html; charset=utf-8"}
;              :body html-with-csrf}))      ;; Return the modified HTML with CSRF token inserted


(defn basic-routes []

  [

   "/" [

        [;["/" {:no-doc false :get {:handler root-handler}}

         ""

         ["sign-in"
          {:no-doc false

           ;; TODO: how to fetch params from request?

           :post {
                  :accept "text/html"

                  :swagger {:produces ["application/multipart-form-data"]}
                  :handler (fn [request]

                             ;; TODO: add validation





                             (let [request-method (:request-method request)
                                   uri (:uri request)

                                   request (assoc request :settings {})


                                   ;; ----------------------------

                                   p (println ">o> >> FORM-DATA??" (get request :form-params))


                                   form-data (get request :form-params)
                                   username (:user form-data)
                                   pw (:password form-data)

                                   ;; process form-validation
                                   resp (if (or (str/blank? username) (str/blank? pw))
                                     (be/create-error-response username request )
                                     (let [
                                           ;; TODO
                                           ;consts/ACTIVATE-DEV-MODE-REDIRECT true
                                           ;consts/ACTIVATE-DEV-MODE-REDIRECT false

                                           p (println ">o> 2RETURN-TO AFTER.0")
                                           p (println ">o> 2RETURN-TO AFTER.a" (get-in request [:form-params :return-to]))
                                           p (println ">o> 2RETURN-TO AFTER.b" (get-in request [:form-params]))
                                           ; FYI: otherwise shared-clj/src/leihs/core/redirects.clj will set
                                           ;resp (when consts/ACTIVATE-DEV-MODE-REDIRECT     (assoc-in response [:headers "Location"] "/new-location/"))

                                           ;; to overrule a redirect, we need to set the return-to value
                                           request (if consts/ACTIVATE-DEV-MODE-REDIRECT (try (assoc-in request [:form-params :return-to] "/inventory/8bd16d45-056d-5590-bc7f-12849f034351/models")
                                                                     (catch Exception e (println "ERROR @ 2RETURN-TO AFTER") request))
                                                                request)

                                           p (println ">o> 2RETURN-TO AFTER" (get-in request [:form-params :return-to]))

                                           ;:return-to} :form-params
                                           ;; ----------------------------


                                           p (println ">o> !!! sign-in POST")
                                           resp (be/routes (convert-params request))

                                           p (println ">o> abc11" (keys request))
                                           p (println ">o> abc12" (:user-session request))
                                           p (println ">o> abc13" (:sessions request))
                                           p (println ">o> abc14" (:token (:query-params-raw request)))
                                           p (println ">o> abc13" (:authenticated-entity request))





                                           created-session (get-in resp [:cookies "leihs-user-session" :value])

                                           ;p (println ">o> abc14.resp.generated" created-session)
                                           ;request (assoc request (:sessions created-session))
                                           ;
                                           ;p (println ">o> abc >> toCHECK!!! :sessions" (get-in request [:sessions]))
                                           ;
                                           ;request (assoc-in request [:cookies "leihs-user-session" :value])
                                           ;p (println ">o> abc >> toCHECK!!!" (get-in request [:cookies "leihs-user-session" :value]))
                                           ; Assign session to request under :sessions
                                           request (assoc request :sessions created-session)

                                           ; Print out the session for verification
                                           p (println ">o> abc >> toCHECK!!! :sessions" (get-in request [:sessions]))

                                           ; Set the :value key for the "leihs-user-session" cookie
                                           request (assoc-in request [:cookies "leihs-user-session" :value] created-session)

                                           ; Print out the cookie value for verification
                                           p (println ">o> abc >> toCHECK!!! :cookies" (get-in request [:cookies "leihs-user-session" :value]))


                                           ;:return-to



                                           p (println ">o> 1RETURN-TO BEFORE" (get-in request [:form-params :return-to]))

                                           ]resp)
                                     )



                                   p (println ">o> !!!!!!! RESP" resp)




                                   ]

                               ;;; Logging request method and URI for debugging
                               ;(println ">o> Request Method:" request-method)
                               ;(println ">o> URI:" uri)


                               (println ">o> ---------- CHECK ----------------")
                               (println ">o> !!! Response:" resp)
                               (println ">o> ---------- CHECK ----------------")

                               resp))}

           :get {
                 :summary "Get sign-in page"
                 :accept "text/html"
                 :swagger {:produces ["text/html"]}

                 :handler get-sign-in

                 ;:handler (fn [request]
                 ;           (let [
                 ;                 mtoken (anti-csrf-token request)
                 ;                 p (println ">o> (html) anti-csrf-token" mtoken)
                 ;
                 ;                 mprops (anti-csrf-props request)
                 ;                 p (println ">o> (html) anti-csrf-props" mprops)
                 ;
                 ;                 uuid mtoken
                 ;
                 ;
                 ;                 ;uuid (str (UUID/randomUUID)) ;; Generate UUID for CSRF token
                 ;                 params {:authFlow {:returnTo "/inventory/models"}
                 ;                         ;:csrfToken {:name "x-csrf-token" ;; should be csrf-token => back
                 ;                         :csrfToken {:name "csrf-token"
                 ;                                     :value uuid}} ;; Parameters including CSRF token
                 ;
                 ;                 html (sign-in-view params) ;; Generate the original HTML using the params
                 ;
                 ;                 ;; Debugging the original HTML
                 ;                 _ (println ">o> html.before" html (type html))
                 ;
                 ;                 ;; Add CSRF tokens to the HTML and debug the result
                 ;                 html-with-csrf (add-csrf-tags html params)
                 ;                 _ (println ">o> html.after" html-with-csrf (type html-with-csrf))
                 ;                 ]
                 ;
                 ;             ;; Return the modified HTML in the response
                 ;             {:status 200
                 ;              :headers {"Content-Type" "text/html; charset=utf-8"}
                 ;              :body html-with-csrf}))      ;; Return the modified HTML with CSRF token inserted

                 }

           }

          ]

         ["sign-out"
          {:no-doc false

           :post {

                  ;:accept "application/json"
                  :accept "text/html"

                  ;:middleware [ab/wrap]
                  :middleware [wrap-authenticate]

                  :handler
                  (fn [request]
                    (let [

                          p (println ">o> !!!!!!!!!!!!!!!!!!! server/routes.clj::POST /sign-out")
                          p (println ">o> server/routes.clj::POST /sign-out")
                          request-method (:request-method request)
                          uri (:uri request)
                          resp (so/routes (convert-params request))]

                      ;; Logging request method and URI for debugging
                      (println ">o> Request Method:" request-method)
                      (println ">o> URI:" uri)
                      ;(println ">o> Response:" resp)

                      resp))}

           :get {
                 :accept "text/html"

                 :handler (fn [request]
                            ;{:status 200
                            ; :headers {"Content-Type" "text/html"}
                            ; :body (sign-in-view {:authFlow {:returnTo "/inventory/models"}})
                            ; }


                            (let [

                                  uuid (get-in request [:cookies constants/ANTI_CSRF_TOKEN_COOKIE_NAME :value])
                                  p (println ">o> logout.uuid" uuid)

                                  ;uuid (str (UUID/randomUUID)) ;; Generate UUID for CSRF token
                                  params {
                                          :authFlow {:returnTo "/inventory/models???/"}
                                          ;:csrfToken {:name "x-csrf-token" ;; should be csrf-token => back
                                          :csrfToken {:name "csrf-token"
                                                      :value uuid}} ;; Parameters including CSRF token
                                  ;html (sign-in-view params) ;; Generate the original HTML using the params

                                  html (slurp (io/resource "public/dev-logout.html"))

                                  ;; Debugging the original HTML
                                  _ (println ">o> html.before" html (type html))

                                  ;; Add CSRF tokens to the HTML and debug the result

                                  ;; TODO: does this work?
                                  html-with-csrf (add-csrf-tags html params)
                                  ;html-with-csrf (add-csrf-tags2 html params)

                                  _ (println ">o> html.after" html-with-csrf (type html-with-csrf))
                                  html html-with-csrf
                                  ]

                              {:status 200
                               :headers {"Content-Type" "text/html"}
                               ;:body (sign-out-view {})}
                               :body html}
                              )

                            )}

           }]

         ]


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

         (incl-other-routes)]

        ]

   ])

;#### debug ###################################################################
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
