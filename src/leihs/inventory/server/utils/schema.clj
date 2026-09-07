(ns leihs.inventory.server.utils.schema
  "Schema type definitions for API coercion."
  (:require
   [clojure.string :as string]
   [schema.core :as s])
  (:import [java.time LocalDate]
           [java.time.format DateTimeFormatter]))

(def ^:private date-formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd"))

(def pagination {:size s/Int
                 :page s/Int
                 :total_rows s/Int
                 :total_pages s/Int})

(def Date java.time.LocalDate)

(def Price (s/constrained s/Num #(< -1000000 % 1000000) 'price-in-range))

(def NonBlankStr (s/constrained s/Str (comp not string/blank?) 'non-blank-string))

(defn instant-to-date-string [date]
  (when date
    (if (instance? LocalDate date)
      (.format ^LocalDate date date-formatter)
      (-> date .toLocalDate (.format date-formatter)))))
