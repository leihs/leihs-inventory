(ns leihs.inventory.server.resources.pool.models.form.common
  (:require
   [cheshire.core :as cjson]
   [clojure.data.codec.base64 :as b64]
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.models.helper :refer [base-filename file-to-base64 normalize-files normalize-model-data
                                                           parse-json-array process-attachments ]]
   [leihs.inventory.server.resources.pool.common :refer [str-to-bool    ]]

   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]])
  (:import [java.net URL JarURLConnection]
           (java.time LocalDateTime)
           [java.util UUID]))

(defn db-operation
  "Executes a SELECT or DELETE operation on the given table based on the operation keyword using next.jdbc and HoneySQL."
  [tx operation table where-clause]
  (let [query (case operation
                :select
                (-> (sql/select :*)
                    (sql/from (keyword table))
                    (sql/where where-clause)
                    sql-format)
                :delete (-> (sql/delete-from table)
                            (sql/where where-clause)
                            sql-format)
                (throw (IllegalArgumentException. "Unsupported operation")))]
    (jdbc/execute! tx query)))

(defn filter-keys
  "Filters the keys of each map in the vector, keeping only the specified keys."
  [vec-of-maps keys-to-keep]
  (mapv #(select-keys % keys-to-keep) vec-of-maps))
