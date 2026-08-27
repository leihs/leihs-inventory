(ns leihs.inventory.client.lib.date-utils
  (:require
   [clojure.string :refer [split]]))

(defn date-from-iso
  "Create a JS Date object with the given date at 0:00:00 in the client's time zone
   (avoiding time-zone-driven date shifting). Returns nil for nil input."
  [date-str]
  (when date-str
    (let [[y m d] (map js/parseInt
                       (split date-str #"-"))
          date (js/Date. y (dec m) d)]
      date)))
