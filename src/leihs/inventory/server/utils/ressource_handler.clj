(ns leihs.inventory.server.utils.ressource-handler
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as clojure.string]
   [clojure.string :as str]
   [leihs.inventory.server.utils.request-utils :refer [authenticated?]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.util.response :refer [response status content-type]]))

(defn pr [str fnc]
  ;(println ">oo> HELPER / " str fnc)(println ">oo> HELPER / " str fnc)
  (println ">oo> " str fnc)
  fnc)

(defn custom-not-found-handler [request]
  (rh/index-html-response request 404))
