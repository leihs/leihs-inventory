(ns leihs.inventory.server.swagger-api
  (:require [byte-streams :as bs]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string]
            [clojure.string :as str]
            [clojure.uuid :as uuid]
            [clojure.walk :refer [keywordize-keys]]
            [leihs.core.anti-csrf.back :as anti-csrf]
            [leihs.core.auth.core :as auth]
            [leihs.core.auth.session :as session]
            [leihs.core.constants :as constants]
            [leihs.core.db :as db]
            [leihs.core.ring-audits :as ring-audits]
            [leihs.core.routing.back :as core-routing]
            [leihs.core.routing.dispatch-content-type :as dispatch-content-type]
            [leihs.core.sign-in.back :as be]
            [leihs.inventory.server.constants :as consts]
            [leihs.inventory.server.routes :as routes]
            [leihs.inventory.server.utils.csrf-handler :as csrf]
            [leihs.inventory.server.utils.response_helper :as rh]
            [leihs.inventory.server.utils.ressource-handler :refer [custom-not-found-handler]]
            [muuntaja.core :as m]
            [reitit.coercion.schema]
   [clojure.string :as str]
            [reitit.coercion.spec]
            [reitit.dev.pretty :as pretty]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.exception :as exception]
            [reitit.ring.middleware.multipart :as multipart]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.util.codec :as codec]
            [ring.util.response :as response]))

(defn pr [str fnc]
  ;(println ">oo> HELPER / " str fnc)(println ">oo> HELPER / " str fnc)
  (println ">oo> " str fnc)
  fnc)
(defn pr2 [str fnc]
  ;(println ">oo> HELPER / " str fnc)(println ">oo> HELPER / " str fnc)
  (println ">oo> " str)
  fnc)



(defn default-handler-fetch-resource [handler]
  (fn [request]
    (let [p (println ">o> cccc3")
          accept-header (get-in request [:headers "accept"])
          p (println ">o> accept-header.old" accept-header)
          p (println ">o> accept-header.uri" (:uri request))
          p (println ">o> accept-header.uri" (:accept request))

          ctype (get-in request [:headers "content-type"])


          uri (:uri request)
          ;whitelist-uris-for-api ["/sign-in" "/sign-out" "/inventory/8bd16d45-056d-5590-bc7f-12849f034351/dev/model"]]
          whitelist-uris-for-api ["/sign-in" "/sign-out" "/inventory/images/fe8e42a0-1d31-4449-8c91-24c17ddd5d10/thumbnail"]


          ;; fixme
          ;request (if (and (str/includes? accept-header "text/html") (str/includes? uri "/thumbnail"))
          ;          ;(assoc request :body (get-in request [:headers "content-type"]) "image/jpeg")
          ;          (pr2 "rewrite to image/jpeg" (assoc-in request [:headers "accept"] "image/jpeg"))
          ;
          ;          request)
          ;accept-header (get-in request [:headers "accept"])
          p (println ">o> accept-header.new" accept-header)



          p (println ">o> abc1")]
      (if (or (some #(clojure.string/includes? accept-header %) ["json" "image/jpeg"])
              (some #(= % uri) whitelist-uris-for-api))
        (pr2 ">1" (handler request))
        (pr2 ">2" (custom-not-found-handler request))))))


(defn valid-inventory-image-uri? [uri]
  (let [pattern #"^/inventory/images/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}(/thumbnail)?$"]
    (boolean (re-matches pattern uri))))


(defn wrap-accept-with-image-rewrite [handler]
  (fn [request]
    (let [accept-header (get-in request [:headers "accept"])
          uri (:uri request)]

      ;; Log the initial values for debugging
      (println ">o> Initial Accept header:" accept-header)
      (println ">o> URI:" uri)

      ;; Conditionally rewrite the Accept header to "image/jpeg" if the URI is valid and Accept includes "text/html"
      (let [updated-request (if (and (str/includes? accept-header "text/html")
                                  (valid-inventory-image-uri? uri))
                              (do
                                (println ">o> Rewriting Accept header to image/jpeg")
                                (assoc-in request [:headers "accept"] "image/jpeg"))
                              request)]

        ;; Log the Accept header after potential rewrite for debugging
        (println ">o> Updated Accept header:" (get-in updated-request [:headers "accept"]))

        ;; Pass the (possibly modified) request to the wrapped handler
        ((dispatch-content-type/wrap-accept handler) updated-request)))))


(defn wrap-accept-with-image-rewrite [handler]
  (fn [request]
    (let [accept-header (get-in request [:headers "accept"])
          uri (:uri request)
          updated-request (if (and (str/includes? accept-header "text/html")
                                (valid-inventory-image-uri? uri))
                            (assoc-in request [:headers "accept"] "image/jpeg")
                            request)]
      ((dispatch-content-type/wrap-accept handler) updated-request))))


(defn create-app [options]
  (let [router (ring/router

                (routes/basic-routes)

                {:conflicts nil
                 :exception pretty/exception
                 :data {:coercion reitit.coercion.spec/coercion
                        :muuntaja m/instance
                        :middleware [db/wrap-tx
                                     core-routing/wrap-canonicalize-params-maps
                                     muuntaja/format-middleware
                                     ring-audits/wrap

                                     ;;; rename to: wrap-accept-with-image-rewrite
                                     ;(fn [handler]
                                     ;  (fn [request]
                                     ;    (let [
                                     ;          accept-header (get-in request [:headers "accept"])
                                     ;
                                     ;          uri (:uri request)
                                     ;
                                     ;          p (println ">o> ?? accept1 ??" (:accept request))
                                     ;          p (println ">o> ?? accept2 ??" (get-in request [:headers "accept"]))
                                     ;          p (println ">o> ?? uri ??" uri)
                                     ;
                                     ;          request (if (and (str/includes? accept-header "text/html") (valid-inventory-image-uri? uri))
                                     ;                    ;(assoc request :body (get-in request [:headers "content-type"]) "image/jpeg")
                                     ;                    (pr2 ">o> ??? rewrite to image/jpeg" (assoc-in request [:headers "accept"] "image/jpeg"))
                                     ;
                                     ;                    request)
                                     ;
                                     ;          p (println ">o> ?? accept1 ??" (:accept request))
                                     ;          p (println ">o> ?? accept2 ??" (get-in request [:headers "accept"]))
                                     ;
                                     ;          res ((dispatch-content-type/wrap-accept handler) request)
                                     ;          ]
                                     ;
                                     ;
                                     ;      res
                                     ;
                                     ;
                                     ;      )))


                                     wrap-accept-with-image-rewrite

                                      ; redirect-if-no-session
                                      ; auth/wrap-authenticate ;broken workflow caused by token

                                     csrf/extract-header
                                     session/wrap-authenticate
                                     wrap-cookies
                                     csrf/wrap-csrf

                                      ;locale/wrap
                                      ;settings/wrap
                                      ;datasource/wrap-tx
                                      ;wrap-json-response
                                      ;(wrap-json-body {:keywords? true})
                                      ;wrap-empty
                                      ;wrap-form-params

                                     wrap-params
                                     wrap-content-type

                                      ;(core-routing/wrap-resolve-handler html/html-handler)
                                     ;dispatch-content-type/wrap-accept

                                      ;ring-exception/wrap

                                     default-handler-fetch-resource ;; provide resources
                                     csrf/wrap-dispatch-content-type



                                     swagger/swagger-feature
                                     parameters/parameters-middleware
                                     muuntaja/format-negotiate-middleware
                                     muuntaja/format-response-middleware

                                     exception/exception-middleware
                                     muuntaja/format-request-middleware
                                     coercion/coerce-response-middleware
                                     coercion/coerce-request-middleware

                                     multipart/multipart-middleware ;; FIXME: this causes issues with http://localhost:3260/inventory/8bd16d45-056d-5590-bc7f-12849f034351/dev/model
                                     ]}})]
    (-> (ring/ring-handler
         router
         (ring/routes
          (ring/redirect-trailing-slash-handler {:method :strip})

          (swagger-ui/create-swagger-ui-handler
           {:path "/inventory/api-docs/"
            :config {:validatorUrl nil
                     :urls [{:name "swagger" :url "swagger.json"}]
                     :urls.primaryName "openapi"
                     :operationsSorter "alpha"}})

          (ring/create-default-handler
           {:not-found (default-handler-fetch-resource custom-not-found-handler)}))))))
