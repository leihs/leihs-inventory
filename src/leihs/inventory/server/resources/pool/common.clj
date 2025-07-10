(ns leihs.inventory.server.resources.pool.common
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
   [taoensso.timbre :refer [debug info warn error spy]])
  (:import [java.math BigDecimal RoundingMode]
           [java.net URL JarURLConnection]
           (java.time LocalDateTime)
           [java.util UUID]
           [java.util.jar JarFile]))

(defn apply-is_deleted-context-if-valid
  "setups base-query for is_deletable and references:
  - m: models
  - i: items
  - it: items (for items that are children of item i)"
  [is_deletable]
  (-> (sql/select-distinct :m.*
                           [[:raw "CASE
                                   WHEN m.is_package = true AND m.type = 'Model' AND i.id IS NULL AND it.id IS NULL THEN true
                                   WHEN m.is_package = false AND m.type = 'Model' AND i.id IS NULL AND it.id IS NULL THEN true
                                   WHEN m.is_package = false AND m.type = 'Software' AND i.id IS NULL AND it.id IS NULL THEN true
                                   ELSE false
                                   END"]
                            :is_deletable])
      (sql/from [:models :m])
      (sql/left-join [:items :i] [:= :m.id :i.model_id])
      (sql/left-join [:items :it] [:= :it.parent_id :i.id])))

(defn apply-is_deleted-where-context-if-valid [base-query is_deletable]
  (if (nil? is_deletable)
    base-query
    (-> (sql/select-distinct :*)
        (sql/from [[base-query] :wrapped_query])
        (sql/where [:= :wrapped_query.is_deletable is_deletable]))))

(defn str-to-bool
  [s]
  (cond
    (string? s) (case (.toLowerCase s)
                  "true" true
                  "false" false
                  nil)
    :else (boolean s)))

(defn- customized-empty? [value]
  (or (= value "null") (nil? value) (empty? value)))

(defn int-to-numeric [int-value]
  (try (-> (BigDecimal/valueOf int-value) (.setScale 2 RoundingMode/HALF_UP))
       (catch Exception e (error "Error in int-to-numeric" e) (BigDecimal. 0))))

(defn int-to-numeric-or-nil [int-value]
  (try (-> (BigDecimal/valueOf int-value) (.setScale 2 RoundingMode/HALF_UP))
       (catch Exception e (error "Error in int-to-numeric" e) nil)))

(defn double-to-numeric-or-zero [int-value]
  (let [parsed-value (if (string? int-value) (Double/parseDouble int-value) int-value)]
    (int-to-numeric parsed-value)))

(defn double-to-numeric-or-zero [int-value]
  (if (customized-empty? int-value)
    0
    (let [parsed-value (if (string? int-value) (Double/parseDouble int-value) int-value)]
      (int-to-numeric parsed-value))))

(defn remove-nil-entries
  "Removes entries from the map if the values of the specified keys are nil."
  [data keys-to-check]
  (reduce (fn [m k]
            (if (nil? (get m k))
              (dissoc m k)
              m))
          data
          keys-to-check))

(def remove-nil-entries-fnc
  (fn [res]
    (mapv
     (fn [r]
       (->> r
            (filter (comp some? val))
            (into {})))
     res)))

(defn keep-attr-not-nil
  "Given a map `m` and a collection of allowed keys `allowed-attrs`,
  return a new map with:
    - only keys present in allowed-attrs
    - all entries with nil values removed"
  [m allowed-attrs]
  (->> m
    (filter (fn [[k v]] (and (some #{k} allowed-attrs) (some? v))))
    (into {})))

(defn remove-empty-entries
  "Removes entries from the map if the values of the specified keys are empty strings."
  [data keys-to-check]
  (reduce (fn [m k]
            (try
              (if (and (instance? String (get m k)) (clojure.string/blank? (get m k)))
                (dissoc m k)
                m)
              (catch Exception e)))
          data
          keys-to-check))

(defn parse-local-date-or-nil
  "Parses a string into a java.time.LocalDate or returns nil if the input is nil or empty."
  [value]
  (if (and value (not (empty? (str value))))
    (java.time.LocalDate/parse value)
    nil))

(defn double-to-numeric-or-nil [int-value]
  (cond
    (nil? int-value) nil
    (instance? java.lang.Double int-value) int-value
    (customized-empty? int-value) nil
    :else (let [parsed-value (if (string? int-value)
                               (try
                                 (Double/parseDouble int-value)
                                 (catch NumberFormatException _ nil))
                               int-value)]
            (int-to-numeric-or-nil parsed-value))))

(defn cast-to-nil [value]
  (if (customized-empty? value) nil value))

(defn cast-to-uuid-or-nil [value]
  (if (customized-empty? value) nil (to-uuid value)))

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

(defn calculate-retired-value
  "Determines the retired value based on database and request values."
  [db-retired request-retired]
  (let [now-ts (LocalDateTime/now)]
    (cond
      (and (boolean? request-retired) (nil? db-retired) request-retired) (.toLocalDate now-ts)
      (and (boolean? request-retired) (nil? db-retired) (not request-retired)) nil
      (and (nil? db-retired) request-retired) (.toLocalDate now-ts)
      (and (not (nil? db-retired)) (not request-retired)) nil
      :else db-retired)))

(defn remove-entries-by-keys
  [m keys-to-remove]
  (reduce dissoc m keys-to-remove))

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

(defn create-validation-response [data validation]
  {:data data
   :validation validation})