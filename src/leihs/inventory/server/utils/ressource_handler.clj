(ns leihs.inventory.server.utils.ressource-handler
  (:require
   [cheshire.core :as json]
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as clojure.string]
   [clojure.string :as str]
   [leihs.inventory.server.utils.request-utils :refer [authenticated?]]
   [leihs.inventory.server.utils.response-helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.util.response :refer [redirect content-type response status content-type]]))

(defn custom-not-found-handler [request]
  (println ">o> abc.custom-not-found-handler")
  (let [accept (str/lower-case (or (get-in request [:headers "accept"]) ""))]
    (cond
      (and (str/includes? accept "text/html") (= (:uri request) "/inventory"))
      (redirect "/inventory/")

      (str/includes? accept "application/json")
      (-> (response (json/generate-string {:error "Not Found" :status 404}))
          (status 404)
          (content-type "application/json; charset=utf-8"))

      (str/includes? accept "text/html")
      (rh/index-html-response request 404)

      :else (-> (response "Not Found")
                (status 404)
                (content-type "text/plain; charset=utf-8")))))