(ns leihs.inventory.server.utils.schema
  "Schema type definitions for API coercion."
  (:require
   [schema.core :as s])
  (:import [java.time LocalDate]
           [java.time.format DateTimeFormatter]))

(def ^:private date-formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd"))

(def pagination {:size s/Int
                 :page s/Int
                 :total_rows s/Int
                 :total_pages s/Int})

(def Date java.time.LocalDate)

(defn instant-to-date-string [date]
  (when date
    (if (instance? LocalDate date)
      (.format ^LocalDate date date-formatter)
      (-> date .toLocalDate (.format date-formatter)))))
