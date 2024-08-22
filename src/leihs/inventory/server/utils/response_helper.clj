(ns leihs.inventory.server.utils.response_helper
  (:require
   [clojure.java.io :as io]
   [clojure.set]
   [ring.middleware.accept]))

(defn index-html-response [status]
  {:status status
   :headers {"Content-Type" "text/html"}
   :body (slurp (io/resource "public/inventory/index.html"))})

(def ^:export INDEX-HTML-RESPONSE-OK (index-html-response 200))
(def ^:export INDEX-HTML-RESPONSE-NOT-FOUND (index-html-response 404))
