(ns leihs.inventory.server.swagger-api
  (:require [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string]
   [clojure.string :as str]
   [clojure.uuid :as uuid]
   [clojure.walk :refer [keywordize-keys]]
   [leihs.core.anti-csrf.back]
   [leihs.core.anti-csrf.back :as anti-csrf]

   [leihs.core.auth.core :as auth]

   [leihs.core.auth.session :as session]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [muuntaja.core :as m]


   [ring.util.codec :as codec]

   [leihs.core.constants :as constants]


   ;[leihs.core.anti-csrf.back :refer [anti-csrf-token anti-csrf-props x-csrf-token!]]
   [leihs.core.core :refer [presence]]

   [leihs.core.db]
   [byte-streams :as bs]

   [leihs.core.db :as db]

   [leihs.core.ring-audits :as ring-audits]


   [leihs.core.routing.back :as core-routing]
   [leihs.core.routing.dispatch-content-type :as dispatch-content-type]
   ;[ring.middleware.params :refer [wrap-params]]

   ;[ring.middleware.form-params :refer [wrap-form-params]];;not work


   ;[ring.middleware.params :refer [wrap-params       wrap-form-params
   ;                                wrap-multipart-params]]


   [leihs.core.sign-in.back :as be]
   [leihs.inventory.server.resources.auth.session :refer [get-cookie-value]]

   ;[leihs.inventory.server.routes :refer [WHITELIST-URIS-FOR-API]]


   [cheshire.core :as json]
   [clojure.java.io :as ioo]

   [leihs.inventory.server.routes :as routes]
   [leihs.inventory.server.utils.response_helper :as rh]
   [leihs.inventory.server.utils.ressource-handler :refer [custom-not-found-handler]]
   [muuntaja.core :as m]
   [reitit.coercion.schema]
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
   ;[ring.middleware.form-params :refer [wrap-form-params]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.util.response :as response])
(:import [java.net URL JarURLConnection]
 [java.util.jar JarFile]
 (java.util UUID)
 ))

(def WHITELIST-URIS-FOR-API ["/sign-in" "/sign-out" "/ab" "/abc"])


(defn pr [str fnc]
  ;(println ">oo> HELPER / " str fnc)(println ">oo> HELPER / " str fnc)
  (println ">oo> " str fnc)
  fnc
  )

(defn default-handler-fetch-resource [handler]
  (fn [request]
    (let [accept-header (get-in request [:headers "accept"])
          p (println ">o> uri=>" (:uri request))
          WHITELIST-URIS-FOR-API ["/sign-in" "/sign-out" "/ab" "/abc"]
          uri (:uri request)
          p (println ">o> che??1" uri WHITELIST-URIS-FOR-API)

          p (println ">o> che??" (some #(= % uri) WHITELIST-URIS-FOR-API))
          ]
      ;(if (some #(clojure.string/includes? accept-header %) ["/json" "image/jpeg"])
      (if (or (some #(clojure.string/includes? accept-header %) ["/json" "image/jpeg"]) (some #(= % uri) WHITELIST-URIS-FOR-API))
        (pr ">o> handler" (handler request))
        ;(pr ">o> custom-fuck" (custom-not-found-handler request))))))
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
       (not (browser-request-matches-javascript? request))) (pr "html-requested!!!" (rh/index-html-response 409))
     :else (let [response (handler request)]
             (if (and (nil? response)
                   (not (#{:post :put :patch :delete} (:request-method request)))
                   (= (-> request :accept :mime) :html)
                   (not (browser-request-matches-javascript? request)))
               (rh/index-html-response 408)
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

(def ANTI_CSRF_TOKEN_COOKIE_NAME "leihs-anti-csrf-token")

(defn valid-uuid? [token]
  (if (nil? token)
    false

    (try
      (UUID/fromString token)                               ;; attempts to parse as UUID, will throw if invalid
      true
      (catch IllegalArgumentException _                     ;; if parsing fails, catch the exception
        false)))
  )


(defn convert-params [request]
  (let [converted-form-params (into {} (map (fn [[k v]] [(clojure.core/keyword k) v]) (:form-params request)))

        request (assoc request :form-params converted-form-params)
        request (assoc request :form-params-raw converted-form-params)

        ]
    ;(assoc request :form-params-raw converted-form-params)
    request
    ))

;(defn extract-form-params [request]
;  (try
;
;                                                         (let [
;                                                               body-stream (:body request)
;                                                               p (println ">o> >1 body-stream" body-stream)
;                                                               body-str (bs/to-string body-stream)
;                                                               p (println ">o> >1 body-str" body-str)
;
;                                                               params (codec/form-decode body-str)
;                                                               p (println ">o> >1 params" params)
;                                                               p (println ">o> >1a params" (:user params))
;                                                               p (println ">o> >1b params" (get params "user")) ;; works
;                                                               p (println ">o> >1c params" (get params :user))
;                                                               ]params)
;
;                                                         (catch Exception e (println ">o> ERROR" (.getMessage e)) nil)))


(defn extract-form-params [request]
  (try
    (let [
          p (println ">o> extract-form-params.IN" (:body request))

          body-stream (:body request)
          _ (println ">o> >1 body-stream" body-stream)

          ;; Convert the body stream to string
          body-str (bs/to-string body-stream)
          _ (println ">o> >1 body-str" body-str)

          ;; Decode form parameters from the string
          params (codec/form-decode body-str)
          _ (println ">o> >1 params" params)

          p (println ">o> extract-form-params.1" params)
          ;; Convert string keys to keywords
          keyword-params (keywordize-keys params)
          _ (println ">o> >1a keyword-params" keyword-params)
          _ (println ">o> >1b keyword-params user:" (:user keyword-params)) ;; This will work now

          p (println ">o> extract-form-params.2.OUT" keyword-params)
          ]
      keyword-params)

    (catch Exception e
      (println ">o> ERROR" (.getMessage e))
      nil)))

(defn extract-header
  [handler]
  (fn [request]

    ;; TODO
    ; anti-csrf-token
    ; x-csrf-token (should be == session.anti-csrf-token)


    (let [;; set :cookies in request


          form-params (:form-params request)
          p (println ">o> abc3" form-params)
          p (println ">o> abc3a" (:parameters request))
          p (println ">o> abc3b" (:params request))
          p (println ">o> abc3c" (:query request))
          p (println ">o> abc3d >>>" (:body request))           ;;here
          p (println ">o> abc3e" (keys request))


          ;(defn anti-csrf-token [request]
          ;  (or (:anti-csrf-token request)
          ;    (-> request
          ;      :cookies
          ;      (get constants/ANTI_CSRF_TOKEN_COOKIE_NAME nil)
          ;      :value
          ;      presence)))

          body (get request :body)
          ;p (println ">o> body21111a" body)
          ;body21111 (try (slurp body) (catch Exception e (println ">o> ERROR" (.getMessage e)) nil))
          ;p (println ">o> body21111b" body21111)
          ;
          ;body21111 (try (slurp (:body request)) (catch Exception e (println ">o> ERROR" (.getMessage e)) nil))
          ;p (println ">o> body21111c" body21111)

          ;body21111 (try (slurp (bs/to-string (:body request))) (catch Exception e (println ">o> ERROR" (.getMessage e)) nil))
          ;body21111 (try
          ;
          ;               (let [
          ;                     body-stream (:body request)
          ;                     p (println ">o> >1 body-stream" body-stream)
          ;            body-str (bs/to-string body-stream)
          ;                     p (println ">o> >1 body-str" body-str)
          ;
          ;                     params (codec/form-decode body-str)
          ;                     p (println ">o> >1 params" params)
          ;                     p (println ">o> >1a params" (:user params))
          ;                     p (println ">o> >1b params" (get params "user")) ;; works
          ;                     p (println ">o> >1c params" (get params :user))
          ;                        ]params)
          ;
          ;            (catch Exception e (println ">o> ERROR" (.getMessage e)) nil))
          ;p (println ">o> body21111d.params" body21111)
          body-form (extract-form-params request)
          p (println ">o> body-form =>" body-form)
          body-token (:x-csrf-token body-form)
          p (println ">o> body-form.token =>" body-token)


          ;(get-in request [:form-params :x-csrf-token])
          request (assoc request :form-params body-form)
          ;request (assoc request :anti-csrf-token body-token)

          p (println ">o> ------------------------------------")



          ;; --------------------------------


          p (println ">o> csrf-token3b BODY-FORM !!!!!! " body-form)
          p (println ">o> csrf-token3b COOKIE2 !!!!!! " (:cookies request))
          p (println ">o> csrf-token3b COOKIE2 !!!!!! " (:anti-csrf-token request))


          request (add-cookies-to-request request)
          p (println ">o> >COOKIE-TOKEN< (:cookies request)" (:cookies request))

          token (get-cookie-value request)


          request (convert-params request)




          p (println ">o> r-params" (:form-params request))
          p (println ">o> r-params" (:form-params-raw request))

          ;request (if (http-unsafe? request)
          ;       (let [
          ;
          ;
          ;             p (println ">o> SET POST CSRF")
          ;
          ;                ])
          ;
          ;
          ;    )


          ;anti-csrf (get-in request [:headers "anti-csrf-token"]) ;; wrapper
          ;request (assoc request :anti-csrf-token anti-csrf) ;; used by wrapper
          ;p (println ">o> >HEADER-TOKEN< (:anti-csrf-token request) => " (:anti-csrf-token request))

          ;head (get-in request [:headers "x-csrf-token"])
          ;body (get-in request [:body "x-csrf-token"])
          ;form (get-in request [:form "x-csrf-token"])
          ;p (println ">o> abc1" head body form)




          body (get-in request [:body])
          ;head (get-in request [:headers "x-csrf-token"])
          form (get-in request [:form])
          p (println ">o> abc2" body form)


          body2 (:body-params request)
p (println ">o> body2" body2)


          ;body-stream (get-in request [:body])
          ;_ (println ">o> abc3a1" body-stream)
          ;body-str    (slurp (io/reader body-stream))  ;; Convert stream to string / BREAKS ALL
          ;_ (println ">o> abc3b1" body-str)
          ;params      (json/parse-string body-str true)  ;; Parse JSON to map
          ;_ (println ">o> abc3c1" params)


          form-params (:form-params request)
          p (println ">o> abc3" form-params)
          p (println ">o> abc3a" (:parameters request))
          p (println ">o> abc3b" (:params request))
          p (println ">o> abc3b" (:query request))
          ;p (println ">o> abc3b" request)


          ;; TODO: REQUEST
          header-x-csrf-token (get-in request [:headers "x-csrf-token"]) ;; meta
          body-x-csrf-token (get-in request [:body "x-csrf-token"]) ;; meta

          x-csrf-token (or header-x-csrf-token body-x-csrf-token)

          ;request (assoc request :x-csrf-token x-csrf-token) ;; used by wrapper

          p (println ">o> >header-x-csrf-token =>" header-x-csrf-token)
          p (println ">o> >body-x-csrf-token =>" body-x-csrf-token)
          p (println ">o> >x-csrf-token =>" x-csrf-token (valid-uuid? x-csrf-token))


          ;x-csrf-token (if (valid-uuid? x-csrf-token)
          ;               x-csrf-token
          ;               nil)
          ;p (println ">o> >x-csrf-token (after-maybe-generated)=>" x-csrf-token)

          ;request (assoc request :anti-csrf-token x-csrf-token) ;; used by wrapper
          ;p (println ">o> >HEADER-TOKEN< (:x-csrf-token request) =>" (:x-csrf-token request))



          ;;; this works
          ;abc (-> request
          ;      :cookies
          ;      (get (keyword ANTI_CSRF_TOKEN_COOKIE_NAME) nil)
          ;      :value
          ;      presence)
          ;p (println ">o> >COOKIE-TOKEN< ABC =>" abc)
          ;p (println ">o> >!!!!TEST, SHOULD BE EQUAL< ABC =>" abc x-csrf-token "O>" (= abc x-csrf-token))
          ;
          ;          abc (-> request
          ;                :cookies
          ;                (get (keyword ANTI_CSRF_TOKEN_COOKIE_NAME) nil)
          ;)
          ;          p (println ">o> >COOKIE-TOKEN< ABC =>" abc)
          ;
          ;          abc (-> request
          ;            :cookies)
          ;          p (println ">o> >COOKIE-TOKEN< ABC =>" abc)


          tx (:tx request)

          ;;; check if token is in cookies and if user-session is valid
          ;request (if-let [token (get-cookie-value request)]
          ;          (if-let [user-session (user-session token tx)]
          ;            (do
          ;              (println ">o> WOW!!! User & session found")
          ;              (assoc request :session {:leihs-anti-csrf-token {:value token}})) ; meta-token of website)
          ;            (do
          ;              (println ">o> 2a.No user-session found")
          ;              request
          ;              )
          ;            )
          ;          (do
          ;            (println ">o> 1a.No cookie-token for user-session  found")
          ;            request
          ;            ))

          ]

      (try
        (handler request)
        (catch Exception e
          (println ">o> ERROR" (.getMessage e))

          (->
            ;(response/response (json/generate-string {:status "failure"
            ;                                          :message "1CSRF-Token/Session not valid"
            ;                                          :detail (.getMessage e)
            ;                                          }))

            (response/response  {:status "failure"
                                                      :message "1CSRF-Token/Session not valid"
                                                      :detail (.getMessage e)
                                                      })
            (response/status 404)
            (response/content-type "application/json"))
          )))))




(defn post-anti-csrf-wrap
  [handler]
  (fn [request]

    ;; TODO
    ; anti-csrf-token
    ; x-csrf-token (should be == session.anti-csrf-token)


    (let [

          p (println ">o> csrf-token3b COOKIE3 !!!!!! " (:cookies request))
          p (println ">o> csrf-token3b COOKIE3 !!!!!! " (:anti-csrf-token request))

          token1 (:cookies request)
          token2 (:anti-csrf-token request)

          max-age 3600
          token (or token2 token1)

          tx (:tx request)

          p (println ">o> token!!!!!!!!!!!!!" token)
          p (println ">o> URI" (:uri request))

          ]

      (try

        ;(handler request)


        (let [res (handler request)

              p (println ">o> response >>>" res)

              ct (get-in res [:headers "Content-Type"])
              p (println ">o> ct >>>" ct)

              ]
          ;; Add anti-CSRF token as a cookie in the response
          ;(response/set-cookie response "anti-csrf-token" anti-csrf-token
          ;  {:http-only true
          ;   :secure true
          ;   :path "/"
          ;   :same-site :strict})

          ;(->
          ;  ;response
          ;
          ;  (response/response (:body res))
          ;    ;{:status "success" :message "User authenticated successfully"})
          ;  (response/set-cookie "leihs-anti-csrf-token" token {:max-age max-age :path "/"})
          ;  (response/set-cookie "leihs-anti-csrf-token2" token {:max-age max-age :path "/inventory"})
          ;
          ;  (response/status (:status res))
          ;  (response/content-type ct)
          ;  ;(response/headers (:headers res))
          ;
          ;  )


          (-> res
            (assoc :cookies {"leihs-anti-csrf-token" {:value token
                                                      :max-age max-age
                                                      :path "/"}
                             "leihs-anti-csrf-token2" {:value token
                                                       :max-age max-age
                                                       :path "/inventory"}})

            (assoc :status 200)

            (update :headers merge (select-keys (:headers res) ["Content-Type"])))

          ;(response/set-cookie "leihs-anti-csrf-token" token {:max-age max-age :path "/"})

          )                                                 ;; Modify options as necessary


        (catch Exception e
          (println ">o> ERROR" (.getMessage e))

          (->
            (response/response (json/generate-string {:status "failure"
                                                      :message "2CSRF-Token/Session not valid"
                                                      :detail (.getMessage e)
                                                      }))
            (response/status 404)
            (response/content-type "application/json")
            )
          )))))









(defn wrapped-anti-csrf
  [handler]
  (let [wrapped-handler (-> handler
                          extract-header
                          anti-csrf/wrap)]
    (fn [request]
      ;; Your main processing logic here
      (println "Inside main wrapped-anti-csrf processing")
      (wrapped-handler request))))                          ;; Use wrapped-handler with all middleware applied


(defn my-before1
  [handler]
  (fn [request]
    (println ">o> my-before1")

    (handler request)

    ))

(defn my-before2
  [handler]
  (fn [request]
    (println ">o> my-before2")
    (handler request)
    ))



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

                                      extract-header


                                      ;auth/wrap-authenticate ;broken workflow caused by token
                                      ;anti-csrf/wrap


                                      my-before1
                                      session/wrap-authenticate
                                      wrap-cookies
                                      my-before2

                                      anti-csrf/wrap


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
