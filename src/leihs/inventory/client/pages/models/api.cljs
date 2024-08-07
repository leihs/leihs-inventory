(ns leihs.inventory.client.pages.models.api
  (:require [cljs-http.client :as http]))

(def base-url "/inventory/models")

(defn get-many []
  (http/get base-url {:headers {"Accept" "application/json"}}))

(defn delete-model [id]
  (http/delete (str base-url "/" id) {:headers {"Accept" "application/json"}}))
