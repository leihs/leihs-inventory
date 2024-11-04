(ns leihs.inventory.server.resources.models.helper
  (:require
   [cheshire.core :as cjson]
   [clojure.data.codec.base64 :as b64]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [next.jdbc :as jdbc]))

(defn str-to-bool
  [s]
  (cond
    (string? s) (case (.toLowerCase s)
                  "true" true
                  "false" false
                  nil)
    :else (boolean s)))

(defn normalize-model-data
  [data]
  (let [key-map {:type :type
                 :manufacturer :manufacturer
                 :product :product
                 :version :version
                 :hand_over_note :importantNotes
                 :is_package :isPackage
                 :description :description
                 :internal_description :internalDescription
                 :technical_detail :technicalDetails}
        normalized-data (reduce (fn [acc [db-key original-key]]
                                  (if-let [val (get data original-key)]
                                    (assoc acc db-key val)
                                    acc))
                                {}
                                key-map)]
    normalized-data))

(defn parse-json-array
  "Parse the JSON string and return the vector of maps. (swagger-ui normalizer)"
  [request key]
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

                    parsed (cjson/parse-string normalized-json-array-string true)
                    parsed-vector (vec parsed)]
                parsed-vector)
              (catch Exception e
                (throw (ex-info "Invalid JSON Array Format" {:error (.getMessage e)})))))))

(defn normalize-files
  [request key]
  (let [attachments (get-in request [:parameters :multipart key])
        normalized (if (map? attachments)
                     [attachments]
                     attachments)]
    (vec (filter #(pos? (:size % 0)) normalized))))

(defn file-to-base64 [file]
  (let [actual-file (if (instance? java.io.File file)
                      file
                      (:tempfile file))]
    (when actual-file
      (let [bytes (with-open [in (io/input-stream actual-file)
                              out (java.io.ByteArrayOutputStream.)]
                    (io/copy in out)
                    (.toByteArray out))]
        (String. (b64/encode bytes))))))

(defn base-filename
  [filename]
  (if-let [[_ base extension] (re-matches #"(.*)_thumb(\.[^.]+)$" filename)]
    (str base extension)
    filename))

;; -------------

(defn process-attachments [tx attachments model-id]
  (doseq [entry attachments]
    (let [file-content (file-to-base64 (:tempfile entry))
          data (assoc (dissoc entry :tempfile) :content file-content :model_id model-id)]
      (jdbc/execute! tx (-> (sql/insert-into :attachments)
                            (sql/values [data])
                            (sql/returning :*)
                            sql-format)))))