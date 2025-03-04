(ns leihs.inventory.server.resources.models.helper
  (:require
   [cheshire.core :as cjson]
   [clojure.data.codec.base64 :as b64]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [honey.sql :as sq :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [next.jdbc :as jdbc])
  (:import (java.security MessageDigest)
           (java.util Base64)))

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

(defn extract-shortname-and-number [code]
  (let [pattern #"^(P-AUS)(\d+)$|^([A-Z]+)(\d+)$"
        matches (re-matches pattern code)]
    (if matches
      (let [shortname (or (nth matches 1) (nth matches 3)) ;; "P-AUS" or normal uppercase letters
            number (or (nth matches 2) (nth matches 4))] ;; Extracted number
        {:shortname shortname
         :number (Integer/parseInt number)})

      (do
        (println (str "Caution: Code format is invalid! Current=" code
                      "\n         Expected formats: 'P-AUS<number>' or 'UPPERCASE followed by digits'"))
        (throw (ex-info "Caution: Format of inventoryCode is invalid!" {:status 500}))
        nil))))

;; TODO: this should be the fixed one
(defn fetch-latest-inventory-code [tx owner-id]
  (let [res (-> (sql/select :items.inventory_code
                            [(sq/call :cast
                                      (sq/call :nullif
                                               (sq/call :regexp_replace :items.inventory_code "^[^0-9]*" "")
                                               "")
                                      :integer)
                             :inventory_number])
                (sql/from :items)
                (sql/where [:not= :items.inventory_code nil])
                (sql/where [:ilike :items.inventory_code "P-%"]) ;; Match only inventory codes starting with 'P-'
                (sql/order-by [:inventory_number :desc] [:items.inventory_code :desc])
                (sql/limit 1)
                sql-format)
        res (jdbc/execute-one! tx res)

        res (if (nil? res)
              (let [default {:next-code "DEFAULT-0001"}]
                (println ">> INFO: no inventory_code found, use default: " (:next-code default))
                default)
              (let [shortname-and-number (extract-shortname-and-number (:inventory_code res))]
                (if shortname-and-number
                  (assoc res :next-code (str (:shortname shortname-and-number) (+ (:number shortname-and-number) 1)))
                  {:error "No valid inventory code found"})))]
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

(defn parse-json-map
  "Parse the JSON string and return a map. (swagger-ui normalizer)"
  [request key]
  (let [json-map-string (get-in request [:parameters :multipart key])]
    (cond
      (not json-map-string) {}
      (and (string? json-map-string) (some #(= json-map-string %) ["" "[]" "{}"])) {}
      :else
      (try
        (let [normalized-json-map-string
              (if (.startsWith json-map-string "[")
                (subs json-map-string 1 (dec (count json-map-string)))
                json-map-string)

              parsed (cjson/parse-string normalized-json-map-string true)]

          (if (map? parsed)
            parsed
            (throw (ex-info "Invalid JSON Object Format" {:parsed parsed}))))
        (catch Exception e
          (throw (ex-info "Invalid JSON Map Format" {:error (.getMessage e)})))))))

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

(defn file-sha256 [file]
  (let [actual-file (if (instance? java.io.File file)
                      file
                      (:tempfile file))]
    (when actual-file
      (with-open [input-stream (io/input-stream actual-file)]
        (let [digest (MessageDigest/getInstance "SHA-256")]
          (loop [buffer (byte-array 8192)
                 bytes-read (.read input-stream buffer)]
            (when (pos? bytes-read)
              (.update digest buffer 0 bytes-read)
              (recur buffer (.read input-stream buffer))))
          (str/join (map #(format "%02x" %) (.digest digest))))))))

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
