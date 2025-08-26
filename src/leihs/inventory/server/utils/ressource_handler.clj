(ns leihs.inventory.server.utils.ressource-handler
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [leihs.core.auth.session :as session]
   [leihs.core.db :as db]
   [leihs.inventory.server.utils.csrf-handler :as csrf]
   [leihs.inventory.server.utils.helper :refer [accept-header-html?]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [leihs.inventory.server.utils.ressource-loader :refer [list-files-in-dir]]
   [leihs.inventory.server.utils.session-dev-mode :as dm]
   [leihs.inventory.server.utils.session-utils :refer [session-valid?]]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.util.response :refer [bad-request response status content-type]]))

(defn custom-not-found-handler [request]
  (let [accept-header (or (get-in request [:headers "accept"]) "")]
    (if (clojure.string/includes? accept-header "application/json")
      (-> {:status "failure" :message "No entry found"}
          (json/generate-string)
          (response)
          (status 404)
          (content-type "application/json; charset=utf-8"))
      (rh/index-html-response request 404))))
