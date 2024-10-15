(ns leihs.inventory.server.utils.response_helper
  (:require
   [clojure.java.io :as io]
   [clojure.set]
   [leihs.core.anti-csrf.back :refer [anti-csrf-token anti-csrf-props]]
   [leihs.core.constants :as constants]
   [leihs.core.core :refer [keyword str presence]]
   [leihs.inventory.server.utils.html-utils :refer [add-csrf-tags]]
   [ring.middleware.accept]
   [ring.util.request :as request]
   [ring.util.response :as response])
  (:import [java.net URL JarURLConnection]
           (java.util UUID)
           [java.util.jar JarFile]))

(defn index-html-response [request status]
  (let [index (io/resource "public/index.html")
        default (io/resource "public/index-fallback.html")
        html (slurp (or index default))
        uuid (anti-csrf-token request)
        params {:authFlow {:returnTo "/inventory/models"}
                :csrfToken {:name "csrf-token" :value uuid}}
        html-with-csrf (add-csrf-tags html params)]
    (-> (response/response html-with-csrf)
        (response/status status)
        (response/content-type "text/html; charset=utf-8"))))
