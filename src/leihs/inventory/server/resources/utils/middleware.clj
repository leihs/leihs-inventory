(ns leihs.inventory.server.resources.utils.middleware
  (:require [clojure.string :as str]
            [leihs.inventory.server.utils.response_helper :as rh]))

(defn accept-json-middleware [handler]
  (fn [request]
    (let [accept-header (get-in request [:headers "accept"])]
      (if (and accept-header (re-matches #"^.*application/json.*$" accept-header))
        (handler request)
        rh/INDEX-HTML-RESPONSE-OK))))

(defn accept-json-image-middleware [handler]
  (fn [request]
    (let [accept-header (get-in request [:headers "accept"])]
      (if (some #(clojure.string/includes? accept-header %) ["/json" "image/"])
        (handler request)
        rh/INDEX-HTML-RESPONSE-OK))))