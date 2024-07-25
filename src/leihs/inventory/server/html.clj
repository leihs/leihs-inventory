(ns leihs.inventory.server.html
  (:require
   [ring.util.response :refer [content-type resource-response status]]))

(defn html-handler
  [_]
  (-> "public/index.html"
      resource-response
      (content-type "text/html")))

(defn not-found-handler
  [request]
  (-> (html-handler request)
      (status 404)))
