(ns leihs.inventory.server.utils.constants
  "Defines application configuration and provides an HTTP handler to serve it."
  (:require
   [clojure.string       :as str]
   [ring.util.response   :as response]
   [cheshire.core        :as json]))


(def config
  {:api
   {:upload-dir "/tmp/"
    :images     {:max-size-mb       8
                 :allowed-file-types ["png" "jpg" "jpeg"]
                 :thumbnail         {:width-px  100
                                     :height-px 100}}
    :attachments {:max-size-mb       100
                  :allowed-file-types ["pdf"]}}})


;(defn get-config
;  "Return the full config map." []
;  config)

(defn config-get
  "Fetch a nested config value. E.g. (config-get :api :images :thumbnail :width-px)"
  [& ks]
  (get-in config ks))
