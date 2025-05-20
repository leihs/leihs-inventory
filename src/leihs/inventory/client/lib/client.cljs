(ns leihs.inventory.client.lib.client
  (:require ["axios" :as axios]
            ["axios-cache-interceptor" :as axios-cache]
            [leihs.inventory.client.lib.csrf :as csrf]))

(def instance
  (.. axios -default (create
                      #js {:baseURL ""
                           :headers #js {"Content-Type" "application/json"
                                         "Accept" "application/json"}
                           :xsrfCookieName csrf/cookie-name
                           :xsrfHeaderName csrf/header-field-name})))

(def http-client (axios-cache/setupCache instance))



