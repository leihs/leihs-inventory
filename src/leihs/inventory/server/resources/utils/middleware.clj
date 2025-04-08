(ns leihs.inventory.server.resources.utils.middleware
  (:require [clojure.string :as str]
            [leihs.inventory.server.utils.response_helper :as rh]
            [leihs.inventory.server.utils.response_helper :refer [index-html-response]]
   [leihs.core.auth.session :as session]
   [leihs.core.auth.token :as token]
            [ring.util.response :as response]))

(defn accept-json-middleware [handler]
  (fn [request]
    (let [accept-header (get-in request [:headers "accept"])]
      (if (and accept-header (re-matches #"^.*application/json.*$" accept-header))
        (handler request)
        (index-html-response request 200)))))

(defn accept-json-image-middleware [handler]
  (fn [request]
    (let [accept-header (get-in request [:headers "accept"])]
      (if (some #(clojure.string/includes? accept-header %) ["/json" "image/"])
        (handler request)
        (index-html-response request 200)))))

(defn wrap-is-admin! [handler]
  (fn [request]
    (let [is-admin (get-in request [:authenticated-entity :is_admin] false)]
      (if is-admin
        (handler request)
        (response/status (response/response {:status "failure" :message "Unauthorized"}) 401)))))

(defn wrap-authenticate! [handler]
  (fn [request]
    (let [
          ;
          ;_ (try
          ;
          ;
          ;    ;(defn wrap-authenticate [handler]
          ;    ;  (fn [request]
          ;    ;    (-> request authenticate handler)))
          ;    session/wrap-authenticate
          ;
          ;    (catch Exception e
          ;      (println "Error in session-authenticate!" e)))
          ;
          ;_ (try
          ;
          ;    ;(defn wrap-authenticate [handler & [opts]]
          ;    token/wrap-authenticate
          ;
          ;    (catch Exception e
          ;      (println "Error in token-authenticate!" e)))
          ;



          auth (get-in request [:authenticated-entity] nil)


          p (println ">o> abc.auth" auth)

          uri (:uri request)
          referer (get-in request [:headers "referer"])


          is-api-request? (if referer
                            (str/includes? referer "/api-docs/")
                            false)

          is-accept-json? (str/includes? (get-in request [:headers "accept"]) "application/json")
          p (println ">o> abc.is-accept-json?" is-accept-json?)

          swagger-resource? (str/includes? uri "/api-docs/")
          whitelisted? (some #(str/includes? uri %) ["/sign-in" "/inventory/login"

                                                     "/inventory/token/public"
                                                     "/inventory/session/public"
                                                     ])


          ]
      ;(if (or auth swagger-resource? whitelisted?)
      ;  (handler request)
      ;  (response/status (response/response {:status "failure" :message "Unauthorized2"}) 404))


      (cond
        (or auth swagger-resource? whitelisted?) (handler request)

        is-accept-json? (response/status (response/response {:status "failure" :message "Unauthorized"}) 403)

        :else         (handler request)


        )


      )))
