(ns leihs.inventory.server.routes
  (:refer-clojure :exclude
   [keyword replace])
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [leihs.core.auth.session :refer [wrap-authenticate]]

   [leihs.core.sign-in.back :as be]

   [leihs.core.sign-in.simple-login :refer [sign-in-view]]

   [leihs.core.sign-out.back :as so]

   ;[leihs.core.sign-out.front :refer [component] :rename {component logout-component}]
   ;[leihs.core.sign-out.simple-logout :refer [sign-out-view]]

   [leihs.core.status :as status]
   ;[hiccup.page :refer [html5]]
   [schema.core :as s]
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
   [reitit.openapi :as openapi]
   [reitit.swagger :as swagger]
   [ring.middleware.accept]
   [ring.util.response]
   [ring.util.response :refer [redirect]]
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
  (let [converted-form-params (into {} (map (fn [[k v]] [(clojure.core/keyword k) v]) (:form-params request)))

        request (assoc request :form-params converted-form-params)
        request (assoc request :form-params-raw converted-form-params)

        ]
    ;(assoc request :form-params-raw converted-form-params)
    request
    ))




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





(defn basic-routes []

  [

   "/" [

        [;["/" {:no-doc false :get {:handler root-handler}}

         ""

         ["sign-in"
          {:no-doc false

           :post {
                  :accept "text/html"
                  :swagger {:produces ["application/octet-stream"]}


                  ;; FIXME:
                  ;:parameters {:body {
                  ;                    :user s/Str
                  ;                    :password s/Str
                  ;                    :return-to s/Str
                  ;                    }}
                  ;:parameters {:form {
                  ;                                       :user s/Str
                  ;                                        :password s/Str
                  ;                                        :return-to s/Str
                  ;                    }}

                  :handler
                  (fn [request]
                    (let [request-method (:request-method request)
                          uri (:uri request)

                          request (assoc request :settings {})

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


                          ]

                      ;; Logging request method and URI for debugging
                      (println ">o> Request Method:" request-method)
                      (println ">o> URI:" uri)
                      ;(println ">o> Response:" resp)

                      resp))}

           :get {
                 :summary "Get sign-in page"
                 :accept "text/html"
                 :swagger {:produces ["text/html"]}
                 :handler (fn [request]
                            {:status 200
                             :headers {"Content-Type" "text/html"}
                             :body (sign-in-view {:authFlow {:returnTo "/inventory/models"}})
                             }

                 ) }

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

                   {:status 200
                                            :headers {"Content-Type" "text/html"}
                                            ;:body (sign-out-view {})}
                                            :body (slurp (io/resource "public/dev-logout.html"))}

                   ) }

  }]




;["abc"
; {:get {:summary "[] OK | Authenticate user by login ( set cookie with token )"
;        :accept "text/html"
;        ;:coercion reitit.coercion.schema/coercion
;        ;:swagger {:security [{:basicAuth []}]}
;        :handler (fn [request]
;                   {:status 200
;                    :headers {"Content-Type" "text/html"}
;                    :body (str "fuuuuuuck 1222")})}}]
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

] )

;#### debug ###################################################################
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
