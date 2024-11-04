(ns leihs.inventory.server.utils.csrf-handler
  (:require
   [byte-streams :as bs]
   [clojure.string :as str]
   [clojure.walk :refer [keywordize-keys]]
   [leihs.core.anti-csrf.back :as anti-csrf]
   [leihs.core.auth.core :as auth]
   [leihs.core.auth.session :as session]
   [leihs.core.constants :as constants]
   [leihs.core.core :refer [presence]]
   [leihs.core.db :as db]
   [leihs.core.json :refer [to-json]]
   [leihs.core.ring-audits :as ring-audits]
   [leihs.core.routing.back :as core-routing]
   [leihs.core.routing.dispatch-content-type :as dispatch-content-type]
   [leihs.core.sign-in.back :as be]
   [leihs.inventory.server.constants :as consts]
   [leihs.inventory.server.routes :as routes]
   [leihs.inventory.server.utils.response_helper :as rh]
   [ring.util.codec :as codec]
   [ring.util.response :as response]))

(def WHITELIST-URIS-FOR-API ["/sign-in" "/sign-out"])

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
          (not (browser-request-matches-javascript? request))) (rh/index-html-response request 409)
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
       (map (fn [[k v]] [(keyword k) {:value v}]))
       (into {})))

(defn add-cookies-to-request
  "Adds parsed cookies to the :cookies key in the request map."
  [request]
  (let [cookie-header (get-in request [:headers "cookie"])
        parsed-cookies (when cookie-header (parse-cookies cookie-header))]
    (assoc request :cookies parsed-cookies)))

(alter-var-root #'constants/ANTI_CSRF_TOKEN_COOKIE_NAME (constantly (keyword "leihs-anti-csrf-token")))
(alter-var-root #'constants/USER_SESSION_COOKIE_NAME (constantly (keyword "leihs-user-session")))

(when (not consts/ACTIVATE-CSRF)
  (alter-var-root #'constants/HTTP_UNSAVE_METHODS (constantly #{}))
  (alter-var-root #'constants/HTTP_SAVE_METHODS (constantly #{:get :head :options :trace :delete :patch :post :put})))

(defn convert-params [request]
  (let [converted-form-params (into {} (map (fn [[k v]] [(clojure.core/keyword k) v]) (:form-params request)))]
    (-> request
        (assoc :form-params converted-form-params)
        (assoc :form-params-raw converted-form-params))))

(defn extract-form-params [stream]
  (try
    (let [
          p (println ">o>a-d stream" stream)

          body-str (bs/to-string stream)
          p (println ">o>b body-str" body-str)

          params (codec/form-decode body-str)
          p (println ">o>c params" params)

          keyword-params (keywordize-keys params)
          p (println ">o>d keyword-params" keyword-params)
          ]
      keyword-params)
    (catch Exception e

       (println ">o> extract-form-params.error" e)
      nil)))


(defn webkit-form-boundary? [s]
  (boolean (re-matches #"^------WebKitFormBoundary[0-9A-Za-z]+--$" s)))


(defn extract-header [handler]
  (fn [request]
    (let [
          content-type (get-in request [:headers "content-type"])
          p (println ">o> ?? content-type" content-type)

          request (if (= content-type "application/x-www-form-urlencoded")
                    (let [
                      form-params (:form-params request)
                      p (println ">o> ?? form-params" form-params)
                      form-params (:form-params-raw request)
                      p (println ">o> ?? :form-params-raw" form-params)

                      p (println ">o> 1(:body request)" (:body request)  (type (:body request)))
                      ;p (println ">o> 2(:body request)" (extract-form-params (:body request)))
                      ;p (println ">o> 2(:body request)" (extract-form-params (:body request)))

                      ;body-form (if (nil? (:body request)) nil (extract-form-params (:body request)))
                      ;
                      ;p (println ">o> ?? body-form1" body-form)

                      body-form (if (nil? (:body request)) nil (extract-form-params (:body request)))
                      p (println ">o> ?? body-form2" body-form)



                      csrf-token (get body-form :x-csrf-token)

                      p (println ">o> csrf-token" csrf-token)
                      p (println ">o> body-form" body-form)

                      request (-> request
                                  (assoc :form-params body-form)
                                  add-cookies-to-request
                                  convert-params)
                          ]
                      request

                      ;(try
                      ;  (handler request)
                      ;  (catch Exception e
                      ;    (println ">o> extract-header.error" (.getMessage e))
                      ;
                      ;    (if (str/includes? (:uri request) "/sign-in")
                      ;      (response/redirect "/sign-in?return-to=%2Finventory&message=CSRF-Token/Session not valid")
                      ;      (-> (response/response {:status "failure"
                      ;                              :message "CSRF-Token/Session not valid"
                      ;                              :detail (.getMessage e)})
                      ;        (response/status 404)
                      ;        (response/content-type "application/json")))))

                      )
                    ;request


                    (-> request
                              ;(assoc :form-params body-form)
                              add-cookies-to-request
                              convert-params
                              )
                    ;(handler request)
                    )






           ]

      ;request

      (try
        (handler request)
        (catch Exception e
          (println ">o> extract-header.error" (.getMessage e))

          (if (str/includes? (:uri request) "/sign-in")
            (response/redirect "/sign-in?return-to=%2Finventory&message=CSRF-Token/Session not valid")
            (-> (response/response {:status "failure"
                                    :message "CSRF-Token/Session not valid"
                                    :detail (.getMessage e)})
                (response/status 404)
                (response/content-type "application/json")))))

      )))

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
              (let [uri (:uri request)]
                (if (str/includes? uri "/sign-in")
                  (response/redirect "/sign-in?return-to=%2Finventory&message=CSRF-Token/Session not valid")
                  {:status 400
                   :headers {"Content-Type" "application/json"}
                   :body (to-json {:message "Error updating password"
                                   :detail (str "error: " (.getMessage e))})}))))
          (handler request))))))
