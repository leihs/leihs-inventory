(ns leihs.inventory.server.utils.ressource-handler
  (:require
   [cheshire.core :as json]
   [clojure.string :as str]
   [leihs.inventory.server.utils.response-helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.util.response :refer [response status content-type]]))

(defn custom-not-found-handler [request]
  (let [accept-header (or (get-in request [:headers "accept"]) "")]
    (if (clojure.string/includes? accept-header "application/json")
      (-> {:status "failure" :message "No entry found"}
          (json/generate-string)
          (response)
          (status 404)
          (content-type "application/json; charset=utf-8"))
      (rh/index-html-response request 404))))
