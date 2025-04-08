(ns leihs.inventory.client.lib.client
  (:require ["axios" :as axios]
            [leihs.inventory.client.lib.csrf :as csrf]))

(def http-client
  (.. axios -default (create
                      #js {:baseURL ""
                           :headers #js {"Content-Type" "application/json"
                                         "Accept" "application/json"}
                           :xsrfCookieName csrf/cookie-name
                           :xsrfHeaderName csrf/header-field-name})))
