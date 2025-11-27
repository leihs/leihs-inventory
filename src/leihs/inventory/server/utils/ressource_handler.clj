(ns leihs.inventory.server.utils.ressource-handler
  (:require
   [clojure.string :as str]
   [leihs.inventory.server.utils.exception-handler :refer [create-response-by-accept]]
   [leihs.inventory.server.utils.response-helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]))

(defn custom-not-found-handler [request]
  (let [accept (str/lower-case (or (get-in request [:headers "accept"]) ""))
        uri (:uri request)
        inventory-route? (str/includes? uri "/inventory")]
    (if (and (str/includes? accept "text/html") inventory-route?)
      (rh/index-html-response request)
      (create-response-by-accept accept 404 {:error "Not Found" :status "failure"}))))
