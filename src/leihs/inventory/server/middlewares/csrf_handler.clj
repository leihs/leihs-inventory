(ns leihs.inventory.server.middlewares.csrf-handler
  (:require
   [clojure.string :as str]
   [leihs.core.anti-csrf.back :as anti-csrf]
   [leihs.core.constants :as constants]
   [leihs.core.json :refer [to-json]]
   [leihs.inventory.server.constants :as consts]
   [leihs.inventory.server.middlewares.debug :refer [log-by-severity]]
   [ring.util.response :as response]))

(alter-var-root #'constants/ANTI_CSRF_TOKEN_COOKIE_NAME (constantly (keyword "leihs-anti-csrf-token")))
(alter-var-root #'constants/USER_SESSION_COOKIE_NAME (constantly (keyword "leihs-user-session")))

(when (not consts/ACTIVATE-CSRF)
  (alter-var-root #'constants/HTTP_UNSAVE_METHODS (constantly #{}))
  (alter-var-root #'constants/HTTP_SAVE_METHODS (constantly #{:get :head :options :trace :delete :patch :post :put})))

(defn wrap-csrf [handler]
  (fn [request]
    (let [uri (:uri request)
          api-request? (and uri (or (str/includes? uri "/api-docs/") (str/includes? uri "/swagger-ui/")))]
      (if api-request?
        (handler request)
        (if (some #(= % (:uri request)) ["/sign-in" "/sign-out" "/inventory/login" "/inventory/csrf-token/"])
          (try
            ((anti-csrf/wrap handler) request)
            (catch Exception e
              (log-by-severity e)
              (let [uri (:uri request)]
                (if (str/includes? uri "/sign-in")
                  (response/redirect "/sign-in?return-to=%2Finventory&message=CSRF-Token/Session not valid")
                  {:status 403
                   :headers {"Content-Type" "application/json"}
                   :body (to-json {:message "Error during CSRF-Token/Session validation"
                                   :details (str "error: " (.getMessage e))})}))))
          (handler request))))))
