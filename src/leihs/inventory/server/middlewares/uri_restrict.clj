(ns leihs.inventory.server.middlewares.uri-restrict
  (:require
   [ring.util.response :as response]))

(defn restrict-uri-middleware
  "Middleware that blocks requests unless URI is explicitly allowed."
  [allowed-uris]
  (fn [handler]
    (fn [request]
      (let [uri (:uri request)]
        (if (some #(= uri %) allowed-uris)
          (handler request)
          (response/status 404))))))
