(ns leihs.inventory.server.resources.models.helper
  (:require
   [cheshire.core :as cjson]
   [clojure.data.codec.base64 :as b64]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [honey.sql :as sq :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
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

(defn extract-shortname-and-number [code]
  (let [matches (re-matches #"([A-Z]+)(\d+)" code)]
    (if matches
      {:shortname (nth matches 1)
       :number (Integer/parseInt (nth matches 2))}
      (do
        (println "Caution: Code format is invalid! Expected format: UPPERCASE followed by digits, e.g., AUS85941")
        nil))))

(defn fetch-latest-inventory-code [tx owner-id]
  (let [res (jdbc/execute-one! tx
                               (-> (sql/select :items.inventory_code)
                                   (sql/from :items)
                                   (sql/where [:= :items.owner_id owner-id])
                                   (sql/order-by [:created_at :desc])
                                   (sql/limit 1)
                                   sql-format))

        res (extract-shortname-and-number (:inventory_code res))
        res (if res
              (assoc res :next-code (str (:shortname res) (+ (:number res) 1)))
              {:error "No inventory code found"})]
    res))

(defn normalize-license-data
  [data]
  (let [key-map {:model_id :software_id}
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

(defn process-attachments
  ([tx attachments model-id]
   (doseq [entry attachments]
     (let [file-content (file-to-base64 (:tempfile entry))
           data (assoc (dissoc entry :tempfile) :content file-content :model_id model-id)]
       (jdbc/execute! tx (-> (sql/insert-into :attachments)
                             (sql/values [data])
                             (sql/returning :*)
                             sql-format)))))

  ([tx attachments col_name id]
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
                         sql-format))))
