(ns leihs.inventory.server.utils.response_helper
  (:require
   [clojure.java.io :as io]
   [clojure.set]
   [ring.middleware.accept]))

(defn index-html-response [status]
  (let [index (io/resource "public/inventory/index.html")
        default (io/resource "public/inventory/index-default.html")]
    {:status status
     :headers {"Content-Type" "text/html"}
     :body (slurp (if (nil? index) default index))}))

(def ^:export INDEX-HTML-RESPONSE-OK (index-html-response 200))
(def ^:export INDEX-HTML-RESPONSE-NOT-FOUND (index-html-response 404))
