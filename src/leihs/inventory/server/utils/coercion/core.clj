(ns leihs.inventory.server.utils.coercion.core
  (:require
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s])
  (:import [java.time.format DateTimeFormatter]))

(def pagination {:size s/Int
                 :page s/Int
                 :total_rows s/Int
                 :total_pages s/Int})

(def Date java.time.LocalDate)

(defn instant-to-date-string [date]
  (when date
    (-> date
        .toLocalDate
        (.format (DateTimeFormatter/ofPattern "yyyy-MM-dd")))))
