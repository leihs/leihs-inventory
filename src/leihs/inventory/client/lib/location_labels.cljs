(ns leihs.inventory.client.lib.location-labels
  (:require
   [clojure.string :as str]))

(defn format-room-label
  [{:keys [name description]}]
  (if (str/blank? description)
    name
    (str name " (" description ")")))

(defn format-building-label
  [{:keys [name code]}]
  (if (str/blank? code)
    name
    (str name " (" code ")")))

(defn room-autocomplete-option
  [item]
  {:value (str (:id item))
   :label (format-room-label item)})
