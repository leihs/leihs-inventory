(ns leihs.inventory.server.utils.middleware
  (:require
   [clojure.string :as str]
   [leihs.inventory.server.utils.response_helper :refer [index-html-response]]
   [ring.util.response :as response]
   [taoensso.timbre :refer [debug spy]]))

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
      (let [uri (:uri request)]
        (if (some #(= uri %) allowed-uris)
          (handler request)
          (response/status 404))))))

(defn accept-json-image-middleware [handler]
  (fn [request]
    (let [accept-header (get-in request [:headers "accept"])]
      (if (some #(clojure.string/includes? accept-header %) ["/json" "image/"])
        (handler request)
        (index-html-response request 200)))))

(def whitelisted-paths
  ["/api-docs/"
   "/sign-in"
   "/inventory/csrf-token/"
   "/inventory/token/public"
   "/inventory/status"
   "/inventory/session/public"])

(defn whitelisted?
  ([uri] (whitelisted? uri []))
  ([uri add-paths]
   (some #(str/includes? uri %)
         (into whitelisted-paths add-paths))))

(defn wrap-authenticate! [handler]
  (fn [request]
    (let [auth (get-in request [:authenticated-entity])
          uri (:uri request)
          is-accept-json? (str/includes? (get-in request [:headers "accept"]) "application/json")]
      (cond
        (or auth (whitelisted? uri))
        (handler request)

        (and (nil? auth) is-accept-json?)
        (do
          (debug "Unauthenticated because of: No authenticated-entity && json accept header")
          (-> {:status "failure" :message "Unauthenticated"}
              response/response
              (response/status 403)))

        :else (handler request)))))
