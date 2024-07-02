(ns leihs.inventory.frontend.pages.models.api
  (:require [cljs-http.client :as http]))

(def base-url
  "http://localhost:3260/inventory/api/models"
  #_"http://localhost:3360/models" ; mock api
  )

(defn get-many []
  (http/get base-url))
