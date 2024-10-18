(ns leihs.inventory.server.utils.converter
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [query-params]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]])
  (:import
   [java.util UUID]))

(defn to-uuid [value]
  (try
    (if (instance? String value) (UUID/fromString value) value)
    (catch Exception e
      value)))