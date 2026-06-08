(ns leihs.inventory.client.lib.date-helper
  (:require
   [clojure.string :refer [split]]))

(defn string-to-plain-date
  "Create a JS Date object with the given date at 0:00:00 in the client's time zone
   (avoiding time-zone-driven date shifting)"
  [date-str]
  (let [[y m d] (map js/parseInt
                     (split date-str #"-"))
        date (js/Date. y (dec m) d)]
    date))
