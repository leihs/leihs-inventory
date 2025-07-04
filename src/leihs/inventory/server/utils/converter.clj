(ns leihs.inventory.server.utils.converter
  (:require
   [ring.middleware.accept])
  (:import
   [java.util UUID]))

(defn to-uuid [value]
  (try
    (if (instance? String value) (UUID/fromString value) value)
    (catch Exception e
      value)))