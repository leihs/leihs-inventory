(ns leihs.inventory.server.resources.utils.middleware
  (:require [clojure.string :as str]
            [leihs.inventory.server.utils.response_helper :as rh]
            [leihs.inventory.server.utils.response_helper :refer [index-html-response]]
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
  "Wraps the handler to check if the user is authenticated OR uri is in whitelist"
  (fn [request]
    (let [auth (get-in request [:authenticated-entity] nil)
          whitelist-uris-as-public ["/sign-in" "/inventory/token/public" "/inventory/session/public"]
          uri (:uri request)
          swagger-resource-request? (and uri (str/includes? uri "/api-docs/"))]
      (if (or auth (some #(str/starts-with? uri %) whitelist-uris-as-public) swagger-resource-request?)
        (handler request)
        (response/status (response/response {:status "failure" :message "Unauthorized"}) 401)))))
