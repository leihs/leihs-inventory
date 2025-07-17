(ns leihs.inventory.server.utils.middleware
  (:require
   [clojure.string :as str]
   [leihs.inventory.server.utils.response_helper :refer [index-html-response]]
   [ring.util.response :as response]))

(defn accept-json-middleware [handler]
  (fn [request]
    (let [accept-header (get-in request [:headers "accept"])]
      (if (and accept-header (re-matches #"^.*application/json.*$" accept-header))
        (handler request)
        (index-html-response request 200)))))

(defn restrict-uri-middleware
  "Middleware that blocks requests unless URI is explicitly allowed."
  [allowed-uris]
  (fn [handler]
    (fn [request]
      (let [uri (:uri request)
            referer (:referer request)]
        (if (some #(= uri %) allowed-uris)
          (handler request)
          (response/status 404))))))

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
    (let [auth (get-in request [:authenticated-entity])
          uri (:uri request)
          referer (get-in request [:headers "referer"])
          is-api-request? (and referer (str/includes? referer "/api-docs/"))
          is-accept-json? (str/includes? (get-in request [:headers "accept"]) "application/json")
          swagger-resource? (str/includes? uri "/api-docs/")
          whitelisted? (some #(str/includes? uri %) ["/sign-in"
                                                     "/inventory/csrf-token/"
                                                     "/inventory/token/public"
                                                     "/inventory/status"
                                                     "/inventory/session/public"])]
      (cond
        (or auth swagger-resource? whitelisted?) (handler request)
        is-accept-json? (response/status (response/response {:status "failure" :message "Unauthorized"}) 403)
        :else (handler request)))))
