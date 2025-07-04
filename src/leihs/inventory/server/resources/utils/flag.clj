(ns leihs.inventory.server.resources.utils.flag
  (:require [clojure.string :as str]
            [leihs.inventory.server.utils.response_helper :as rh]
            [leihs.inventory.server.utils.response_helper :refer [index-html-response]]
            [ring.util.response :as response]))

;[leihs.inventory.server.resources.utils.flag :refer [session admin]]
(defn session [s]
  (str "Session: " s))

(defn admin [s]
  (str "Session/Admin: " s))

