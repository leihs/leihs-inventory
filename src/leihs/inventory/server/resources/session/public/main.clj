(ns leihs.inventory.server.resources.session.public.main
  ;(:require
  ; [ring.util.response :as response]
  ; [schema.core :as s])
  ;(:import (com.google.common.io BaseEncoding)
  ;         (java.time Duration Instant)
  ;         (java.util Base64 UUID))
  )

(defn public-endpoint-handler [request]
  {:status 200
   :body {:reuqest-method (:request-method request)
          :request-url (:uri request)
          :message "Hello, World!"}})
