(ns leihs.inventory.server.resources.token.public.main
  ;(:require
  ; [next.jdbc :as jdbc]
  ; [reitit.coercion.schema]
  ; [reitit.coercion.spec]
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
