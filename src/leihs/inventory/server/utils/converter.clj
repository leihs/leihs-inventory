(ns leihs.inventory.server.utils.converter
  (:require
   [ring.middleware.accept])
  (:import
   [java.util UUID]))

(defn to-uuid [x]
  (cond
    (nil? x) nil
    (uuid? x) x
    (string? x) (UUID/fromString x)
    (sequential? x) (mapv to-uuid x)
    :else (throw (ex-info "Unsupported type for uuid conversion"
                          {:value x
                           :type (type x)}))))
