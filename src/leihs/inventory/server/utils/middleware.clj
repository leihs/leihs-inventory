(ns leihs.inventory.server.utils.middleware
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
