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

(defn index-html-response [status]
  (let [index (io/resource "public/inventory/index.html")
        default (io/resource "public/index-fallback.html")]
    {:status status
     :headers {"Content-Type" "text/html"}
     :body (slurp (if (nil? index) default index))}))

(def ^:export INDEX-HTML-RESPONSE-OK (index-html-response 200))
(def ^:export INDEX-HTML-RESPONSE-NOT-FOUND (index-html-response 404))
