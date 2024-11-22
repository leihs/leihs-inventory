(ns leihs.inventory.server.resources.models.form.license.common
  (:require
   [cheshire.core :as cjson]
   [cheshire.core :as jsonc]
   [clojure.data.codec.base64 :as b64]
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]])
  (:import [java.math BigDecimal RoundingMode]
           [java.net URL JarURLConnection]
           (java.time LocalDateTime)
           [java.util UUID]
           [java.util.jar JarFile]))

(defn int-to-numeric [int-value]
  (try (-> (BigDecimal/valueOf int-value) (.setScale 2 RoundingMode/HALF_UP))
       (catch Exception e (println "Error in int-to-numeric" e) (BigDecimal. 0))))

(defn int-to-numeric-or-nil [int-value]
  (try (-> (BigDecimal/valueOf int-value) (.setScale 2 RoundingMode/HALF_UP))
       (catch Exception e (println "Error in int-to-numeric" e) nil)))

(defn double-to-numeric-or-zero [int-value]
  (let [parsed-value (if (string? int-value) (Double/parseDouble int-value) int-value)]
    (int-to-numeric parsed-value)))

(defn double-to-numeric-or-nil [int-value]
  (let [parsed-value (if (string? int-value) (Double/parseDouble int-value) int-value)]
    (int-to-numeric-or-nil parsed-value)))

(defn cast-to-nil [value]
  (if (or (= value "null") (nil? value) (empty? value)) nil value))

(defn cast-to-nil-or-uuid [value]
  (if (or (= value "null") (nil? value) (empty? value)) nil (to-uuid value)))

(defn fetch-default-room-id [tx]
  (let [query (-> (sql/select [:r.id :room_id] [:r.name :room_name] [:b.name :building_name])
                  (sql/from [:rooms :r])
                  (sql/join [:buildings :b] [:= :r.building_id :b.id])
                  (sql/where [:and [:= :b.name "Unbekanntes Gebäude"] [:= :r.name "nicht bekannt"]])
                  sql-format)]
    (jdbc/execute-one! tx query)))

(defn remove-empty-or-nil [m]
  (into {} (filter (fn [[_ v]] (not (or (nil? v) (= v "")))) m)))

(defn parse-json-array [request key]
  (let [json-array-string (get-in request [:parameters :multipart key])]
    (cond
      (not json-array-string) []
      (and (string? json-array-string) (some #(= json-array-string %) ["" "[]" "{}"])) []
      :else (try
              (let [normalized-json-array-string
                    (if (and (.startsWith json-array-string "{")
                             (not (.startsWith json-array-string "[")))
                      (str "[" json-array-string "]")
                      json-array-string)
                    parsed (jsonc/parse-string normalized-json-array-string true)
                    parsed-vector (vec parsed)]
                parsed-vector)
              (catch Exception e
                (throw (ex-info "Invalid JSON Array Format" {:error (.getMessage e)})))))))

(defn normalize-files [request key]
  (let [attachments (get-in request [:parameters :multipart key])
        normalized (if (map? attachments) [attachments] attachments)]
    (vec (filter #(pos? (:size % 0)) normalized))))

(defn file-to-base64 [file]
  (let [actual-file (if (instance? java.io.File file) file (:tempfile file))]
    (when actual-file
      (let [bytes (with-open [in (io/input-stream actual-file)
                              out (java.io.ByteArrayOutputStream.)]
                    (io/copy in out)
                    (.toByteArray out))]
        (String. (b64/encode bytes))))))

(defn base-filename [filename]
  (if-let [[_ base extension] (re-matches #"(.*)_thumb(\.[^.]+)$" filename)]
    (str base extension)
    filename))

(defn process-attachments [tx attachments col_name id]
  (doseq [entry attachments]
    (let [id (to-uuid id)
          file-content (file-to-base64 (:tempfile entry))
          data (assoc (dissoc entry :tempfile) :content file-content (keyword col_name) id)]
      (jdbc/execute! tx (-> (sql/insert-into :attachments)
                            (sql/values [data])
                            (sql/returning :*)
                            sql-format))))
  (jdbc/execute! tx (-> (sql/select :id :filename :content_type :size)
                        (sql/from :attachments)
                        (sql/where [:= (keyword col_name) (to-uuid id)])
                        sql-format)))
