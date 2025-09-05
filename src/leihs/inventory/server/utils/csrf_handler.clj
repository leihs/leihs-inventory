(ns leihs.inventory.server.utils.csrf-handler
  (:require
   [byte-streams :as bs]
   [clojure.string :as str]
   [clojure.walk :refer [keywordize-keys]]
   [leihs.core.anti-csrf.back :as anti-csrf]
   [leihs.core.constants :as constants]
   [leihs.core.json :refer [to-json]]
   [leihs.inventory.server.constants :as consts]
   [leihs.inventory.server.resources.main :refer [get-sign-in]]
   [ring.util.codec :as codec]
   [ring.util.response :as response]))

(defn parse-cookies [cookie-header]
  (->> (str/split cookie-header #"; ")
       (map #(str/split % #"="))
       (map (fn [[k v]] [(keyword k) {:value v}]))
       (into {})))

(defn add-cookies-to-request [request]
  (let [cookie-header (get-in request [:headers "cookie"])
        parsed-cookies (when cookie-header (parse-cookies cookie-header))]
    (assoc request :cookies parsed-cookies)))

(alter-var-root #'constants/ANTI_CSRF_TOKEN_COOKIE_NAME (constantly (keyword "leihs-anti-csrf-token")))
(alter-var-root #'constants/USER_SESSION_COOKIE_NAME (constantly (keyword "leihs-user-session")))

(when (not consts/ACTIVATE-CSRF)
  (alter-var-root #'constants/HTTP_UNSAVE_METHODS (constantly #{}))
  (alter-var-root #'constants/HTTP_SAVE_METHODS (constantly #{:get :head :options :trace :delete :patch :post :put})))

(defn convert-params [request]
  (let [converted-form-params (into {} (map (fn [[k v]] [(keyword k) v]) (:form-params request)))]
    (-> request
        (assoc :form-params converted-form-params)
        (assoc :form-params-raw converted-form-params))))

(defn extract-form-params [stream]
  (try
    (let [body-str (bs/to-string stream)
          params (codec/form-decode body-str)
          keyword-params (keywordize-keys params)]
      keyword-params)
    (catch Exception _ nil)))

(defn extract-header [handler]
  (fn [request]
    (let [content-type (get-in request [:headers "content-type"])
          request (-> request
                      (cond-> (= content-type "application/x-www-form-urlencoded")
                        (assoc :form-params (some-> (:body request) extract-form-params)))
                      add-cookies-to-request
                      convert-params)]
      (try
        (handler request)
        (catch Throwable e
          (if (instance? Throwable e)
            (if (str/includes? (:uri request) "/sign-in")
              (get-sign-in request)
              (-> (response/response {:status "failure"
                                      :message "CSRF-Token/Session not valid"
                                      :details (.getMessage e)})
                  (response/status 403)))
            (response/status 404))))))) ;; coercion error for undefined urls

(defn wrap-csrf [handler]
  (fn [request]
    (let [uri (:uri request)
          api-request? (and uri (str/includes? uri "/api-docs/"))]
      (if api-request?
        (handler request)
        (if (some #(= % (:uri request)) ["/sign-in" "/sign-out" "/inventory/login" "/inventory/csrf-token/"])
          (try
            ((anti-csrf/wrap handler) request)
            (catch Exception e
              (let [uri (:uri request)]
                (if (str/includes? uri "/sign-in")
                  (response/redirect "/sign-in?return-to=%2Finventory&message=CSRF-Token/Session not valid")
                  {:status 400
                   :headers {"Content-Type" "application/json"}
                   :body (to-json {:message "Error updating password"
                                   :details (str "error: " (.getMessage e))})}))))
          (handler request))))))
