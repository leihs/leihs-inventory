(ns leihs.inventory.server.utils.middleware
  (:require
   [clojure.string :as str]
   [leihs.inventory.server.utils.response-helper :refer [index-html-response]]
   [ring.util.response :as response]
   [taoensso.timbre :refer [debug]]))

(defn accept-json-middleware [handler]
  (fn [request]
    (let [accept-header (get-in request [:headers "accept"])
          json-request? (boolean (re-matches #"^.*application/json.*$" accept-header))]
      (if (and accept-header json-request?)
        (handler request)
        (index-html-response request 200)))))

(defn restrict-uri-middleware
  "Middleware that blocks requests unless URI is explicitly allowed."
  [allowed-uris]
  (fn [handler]
    (fn [request]
      (let [uri (:uri request)]
        (if (some #(= uri %) allowed-uris)
          (handler request)
          (response/status 404))))))

(defn wrap-authenticate! [handler]
  (fn [request]
    (let [auth (get-in request [:authenticated-entity])
          uri (:uri request)
          is-accept-json? (str/includes? (get-in request [:headers "accept"]) "application/json")
          swagger-resource? (str/includes? uri "/api-docs/")
          whitelisted? (some #(str/includes? uri %) ["/sign-in"
                                                     "/inventory/csrf-token/"
                                                     "/inventory/token/public"
                                                     "/inventory/status"
                                                     "/inventory/session/public"])]
      (cond
        (or auth swagger-resource? whitelisted?) (handler request)
        (and (nil? auth) is-accept-json?) (do
                                            (debug "Unauthorized because of: No authenticated-entity && json accept header")
                                            (response/status (response/response {:status "failure" :message "Unauthorized"}) 403))
        :else (handler request)))))
