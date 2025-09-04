(ns leihs.inventory.server.resources.pool.models.helper
  (:require
   [cheshire.core :as cjson]
   [clojure.data.codec.base64 :as b64]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [honey.sql :as sq :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [next.jdbc :as jdbc]
   [taoensso.timbre :refer [error info]])
  (:import
   (java.security MessageDigest)))

(defn normalize-model-data
  [data]
  (let [key-map {:type :type
                 :manufacturer :manufacturer
                 :product :product
                 :version :version
                 :is_package :is_package
                 :description :description
                 :technical_detail :technical_detail
                 :internal_description :internal_description
                 :hand_over_note :hand_over_note}
        normalized-data (reduce (fn [acc [db-key original-key]]
                                  (if-let [val (get data original-key)]
                                    (assoc acc db-key val)
                                    acc))
                                {}
                                key-map)]
    normalized-data))
