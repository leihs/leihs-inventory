(ns leihs.inventory.server.swagger-api
  (:require [byte-streams :as bs]
   [cheshire.core :as json]
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.java.io :as ioo]
   [clojure.string]
   [clojure.string :as str]

   [clojure.uuid :as uuid]

   [clojure.walk :refer [keywordize-keys]]
   [leihs.core.anti-csrf.back :as anti-csrf]
   [leihs.core.auth.core :as auth]

   [leihs.core.auth.session :as session]

   [leihs.core.constants :as constants]
   [leihs.core.core :refer [presence]]

   [leihs.core.db]

   [leihs.core.json :refer [to-json from-json ]]

   [leihs.inventory.server.routes :refer [get-sign-in]]


   ;[leihs.core.anti-csrf.back :refer [anti-csrf-token anti-csrf-props x-csrf-token!]]
   [leihs.core.db :as db]

   [leihs.core.ring-audits :as ring-audits]
   [leihs.core.routing.back :as core-routing]

   [leihs.core.routing.dispatch-content-type :as dispatch-content-type]

   [leihs.core.sign-in.back :as be]


   [leihs.inventory.server.constants :as consts]
   [leihs.inventory.server.resources.auth.session :refer [get-cookie-value]]
   ;[ring.middleware.params :refer [wrap-params]]

   ;[ring.middleware.form-params :refer [wrap-form-params]];;not work


   ;[ring.middleware.params :refer [wrap-params       wrap-form-params
   ;                                wrap-multipart-params]]


   [leihs.inventory.server.routes :as routes]
   [leihs.inventory.server.utils.html-utils :refer [add-or-create-return-to-tag]]

   ;[leihs.inventory.server.routes :refer [WHITELIST-URIS-FOR-API]]


   [leihs.inventory.server.utils.response_helper :as rh]
   [leihs.inventory.server.utils.ressource-handler :refer [custom-not-found-handler]]

   [muuntaja.core :as m]
   [muuntaja.core :as m]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [reitit.dev.pretty :as pretty]
   [reitit.ring :as ring]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.exception :as exception]
   [reitit.ring.middleware.multipart :as multipart]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.cookies :refer [wrap-cookies]]
   [ring.middleware.params :refer [wrap-params]]
   ;[ring.middleware.form-params :refer [wrap-form-params]]
   [ring.util.codec :as codec]
   [ring.util.response :as response])
(:import [java.net URL JarURLConnection]
 [java.util.jar JarFile]
 (java.util UUID)
 ))

(def WHITELIST-URIS-FOR-API ["/sign-in" "/sign-out"])


(defn pr [str fnc]
  ;(println ">oo> HELPER / " str fnc)(println ">oo> HELPER / " str fnc)
  (println ">oo> " str fnc)
  fnc  )

(defn default-handler-fetch-resource [handler]
  (fn [request]
    (let [accept-header (get-in request [:headers "accept"])
          uri (:uri request)
          whitelist-uris-for-api ["/sign-in" "/sign-out"]]
      (if (or (some #(clojure.string/includes? accept-header %) ["json" "image/jpeg"])
            (some #(= % uri) whitelist-uris-for-api))
        (handler request)
        (custom-not-found-handler request)))))

(defn browser-request-matches-javascript? [request]
  "Returns true if the accepted type is javascript or
  if the :uri ends with .js. Note that browsers do not
  use the proper accept type for javascript script tags."
  (boolean (or (= (-> request :accept :mime) :javascript)
             (re-find #".+\.js$" (or (-> request :uri presence) "")))))

(defn wrap-dispatch-content-type
  ([handler]
   (fn [request]
     (wrap-dispatch-content-type handler request)))
  ([handler request]
   (cond
     (some #(= % (:uri request)) WHITELIST-URIS-FOR-API) (handler request)

     (= (-> request :accept :mime) :json) (or (handler request)
                                            (throw (ex-info "This resource does not provide a json response."
                                                     {:status 407})))
     (and (= (-> request :accept :mime) :html)
       (#{:get :head} (:request-method request))
       (not (browser-request-matches-javascript? request))) (pr "html-requested!!!" (rh/index-html-response request 409))
     :else (let [response (handler request)]
             (if (and (nil? response)
                   (not (#{:post :put :patch :delete} (:request-method request)))
                   (= (-> request :accept :mime) :html)
                   (not (browser-request-matches-javascript? request)))
               (rh/index-html-response request 408)
               response)))))


(defn parse-cookies
  "Parses cookies from the 'cookie' header string into a nested map."
  [cookie-header]
  (->> (str/split cookie-header #"; ")
    (map #(str/split % #"="))
    (map (fn [[k v]] [(keyword k) {:value v}]))             ;; Format cookies as {:cookie-name {:value "cookie-value"}}
    (into {})))

(defn add-cookies-to-request
  "Adds parsed cookies to the :cookies key in the request map."
  [request]
  (let [cookie-header (get-in request [:headers "cookie"])
        parsed-cookies (when cookie-header (parse-cookies cookie-header))]
    (assoc request :cookies parsed-cookies)))


(defn pr [str fnc]
  ;(println ">oo> HELPER / " str fnc)(println ">oo> HELPER / " str fnc)
  (println ">oo> " str fnc)
  fnc
  )


;; TODO: has to be overwritten
(alter-var-root #'constants/ANTI_CSRF_TOKEN_COOKIE_NAME (constantly (keyword "leihs-anti-csrf-token")))
(alter-var-root #'constants/USER_SESSION_COOKIE_NAME (constantly (keyword "leihs-user-session")))

(when (not consts/ACTIVATE-CSRF)
  (alter-var-root #'constants/HTTP_UNSAVE_METHODS (constantly #{}))
  (alter-var-root #'constants/HTTP_SAVE_METHODS (constantly #{:get :head :options :trace :delete :patch :post :put})))

;; TODO: FOR TESTING
; 1) cookie.anti-csrf-token cookie not set
; 2) website.x-csrf-token != cookie.anti-csrf-token
;(alter-var-root #'constants/HTTP_SAVE_METHODS (constantly #{:head :options :trace}))


;curl --location 'http://localhost:3260/inventory/session/public-csrf' \
;--header 'Accept: application/json' \
;--header 'x-csrf-token: 34ff7872-52bb-4e52-bf6f-4cdf1ccfcff5' \
;--header 'Cookie: depr_leihs-anti-csrf-token=8170e2b8-a266-4224-b86c-37fee48628ca; leihs-user-session=2a44048c-d049-4312-aecd-28c9c3528d6e'

;curl --location 'http://localhost:3260/inventory/session/public-csrf' \
;--header 'Accept: application/json' \
;--header 'anti-csrf-token: 12ff7872-52bb-4e52-bf6f-4cdf1ccfcff5' \
;--header 'x-csrf-token: 34ff7872-52bb-4e52-bf6f-4cdf1ccfcff5' \
;--header 'Cookie: leihs-anti-csrf-token=8170e2b8-a266-4224-b86c-37fee48628ca; leihs-user-session=2a44048c-d049-4312-aecd-28c9c3528d6e'


;>o> ERROR The x-csrf-token is not equal to the anti-csrf cookie value.
;>o> ERROR The x-csrf-token has not been send!
;>o> ERROR The anti-csrf-token cookie value is not set.


(defn valid-uuid? [token]
  (if (nil? token)
    false
    (try
      (UUID/fromString token)                               ;; attempts to parse as UUID, will throw if invalid
      true
      (catch IllegalArgumentException _                     ;; if parsing fails, catch the exception
        false)))  )

(defn convert-params [request]
  (let [converted-form-params (into {} (map (fn [[k v]] [(clojure.core/keyword k) v]) (:form-params request)))]
    (-> request
      (assoc :form-params converted-form-params)
      (assoc :form-params-raw converted-form-params))))

(defn extract-form-params [stream]
  (try
    (let [body-str (bs/to-string stream)
          params (codec/form-decode body-str)
          keyword-params (keywordize-keys params)]
      keyword-params)
    (catch Exception e
      (println ">o> ERROR" (.getMessage e))
      nil)))

(defn extract-header [handler]
  (fn [request]
    (let [form-params (:form-params request)
          body-form (extract-form-params (:body request))
          csrf-token (get body-form :x-csrf-token)
          request (-> request
                    (assoc :form-params body-form)
                    add-cookies-to-request
                    convert-params)]
      (try
        (handler request)
        (catch Exception e
          (println ">o> ERROR" (.getMessage e))
          (if (str/includes? (:uri request) "/sign-in")
            (response/redirect "/sign-in?return-to=%2Finventory&message=CSRF-Token/Session not valid")
            (-> (response/response {:status "failure"
                                    :message "CSRF-Token/Session not valid"
                                    :detail (.getMessage e)})
              (response/status 402)
              (response/content-type "application/json"))))))))

(defn wrap-csrf [handler]
  (fn [request]
    (let [referer (get-in request [:headers "referer"])
          api-request? (and referer (str/includes? referer "/api-docs/"))]
      (if api-request?
        (handler request)
        (if (some #(= % (:uri request)) ["/sign-in" "/sign-out"])
          (try
            ((anti-csrf/wrap handler) request)
            (catch Exception e
              {:status 400
               :headers {"Content-Type" "application/json"}
               :body (to-json {:message "Error updating password"
                               :detail (.getMessage e)})}))
          (handler request))))))

(defn create-app [options]
  (let [router (ring/router

                 (routes/basic-routes)

                 {:conflicts nil
                  :exception pretty/exception
                  :data {:coercion reitit.coercion.spec/coercion
                         :muuntaja m/instance
                         :middleware [db/wrap-tx
                                      core-routing/wrap-canonicalize-params-maps

                                      ; redirect-if-no-session


                                      muuntaja/format-middleware


                                      ring-audits/wrap

                                      ;auth/wrap-authenticate ;broken workflow caused by token

                                      ;; order#1
                                      ;; HINT: csrf-handling
                                      extract-header
                                      ;; order#2
                                      session/wrap-authenticate
                                      wrap-cookies
                                      ;; order#3
                                      wrap-csrf

                                      ;locale/wrap
                                      ;settings/wrap
                                      ;datasource/wrap-tx
                                      ;wrap-json-response
                                      ;(wrap-json-body {:keywords? true})
                                      ;wrap-empty
                                      ;wrap-form-params

                                      wrap-params
                                      ;wrap-form-params

                                      ;wrap-multipart-params
                                      wrap-content-type

                                      ;(core-routing/wrap-resolve-handler html/html-handler)
                                      dispatch-content-type/wrap-accept
                                      ;ring-exception/wrap

                                      default-handler-fetch-resource ;; provide resources
                                      wrap-dispatch-content-type

                                      swagger/swagger-feature
                                      parameters/parameters-middleware
                                      muuntaja/format-negotiate-middleware
                                      muuntaja/format-response-middleware
                                      exception/exception-middleware
                                      muuntaja/format-request-middleware
                                      coercion/coerce-response-middleware
                                      coercion/coerce-request-middleware
                                      multipart/multipart-middleware]}})]

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
