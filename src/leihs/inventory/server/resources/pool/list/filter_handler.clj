(ns leihs.inventory.server.resources.pool.list.filter-handler
  (:require
   [cheshire.core :as json]
   [clojure.edn :as edn]
   [clojure.set :as set]
   [clojure.string :as str]
   [honey.sql.helpers :as sql])
  (:import [com.fasterxml.jackson.core JsonParseException]))

(defn uuid-string?
  "True if v is a UUID-formatted string."
  [v]
  (and (string? v)
       (re-matches
        #"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
        v)))

(defn cast-uuid
  "Casts UUID strings to [:cast v :uuid] for HoneySQL.
   Works on scalars and collections."
  [v]
  (cond
    ;; multiple values (e.g. IN filters)
    (sequential? v)
    (mapv #(if (uuid-string? %) [:cast % :uuid] %) v)

    ;; single UUID
    (uuid-string? v)
    [:cast v :uuid]

    ;; default passthrough
    :else
    v))

;; ------------------------------------------------------------
;; Parse compact JSON: always a vector of maps (more tolerant)
;; ------------------------------------------------------------

(defn parse-json-param [s]
  (println ">>>>> parse-json-param: raw input type:" (type s))
  (println ">>>>> parse-json-param: raw input value:" s)
  (try
    (let [trimmed (-> s str str/trim)
          parsed (cond
                   ;; already decoded EDN/JSON
                   (vector? s) s
                   (map? s) [s]

                   :else
                   (try
                     (json/parse-string trimmed true)
                     (catch Exception e-json
                       (println ">>>>> JSON parse failed:" (.getMessage e-json))
                       (edn/read-string trimmed))))]
      (println ">>>>> parsed raw:" (pr-str parsed))
      (let [v (cond
                (vector? parsed) parsed
                (seq? parsed) (vec parsed)
                (map? parsed) [parsed]
                :else nil)]
        (println ">>>>> normalized to vector:" (pr-str v))
        (if (and (vector? v) (every? map? v))
          v
          (throw (ex-info "Invalid filter format: must be a vector of maps"
                          {:status 400 :type :invalid-structure})))))
    (catch Exception e
      (println ">>>>> parse-json-param final exception:" (.getMessage e))
      (throw (ex-info "Malformed or invalid filter input."
                      {:status 400
                       :type :parse-error
                       :cause (.getMessage e)})))))

(defn parse-json-param [s]
  (println ">>>>> parse-json-param: raw input type:" (type s))
  (println ">>>>> parse-json-param: raw input value:" s)
  (try
    (cond
      ;; nil or empty string → nothing to parse
      (or (nil? s)
          (and (string? s)
               (str/blank? s)))      (do
        (println ">>>>> parse-json-param: empty or nil input — returning nil")
        nil)

      (and (vector? s) (every? map? s))       s

      (map? s)      [s]

      :else       (let [trimmed (str/trim (str s))
            parsed (try
                     (json/parse-string trimmed true)
                     (catch Exception _
                       (try
                         (edn/read-string trimmed)
                         (catch Exception _
                           trimmed)))) ; fallback to raw string
            v (cond
                (vector? parsed) parsed
                (seq? parsed) (vec parsed)
                (map? parsed) [parsed]
                :else nil)]
        (println ">>>>> parsed raw:" (pr-str parsed))
        (println ">>>>> normalized to vector:" (pr-str v))
        (if (and (vector? v) (every? map? v))
          v
          (do
            (println ">>>>> parse-json-param: returning scalar string or nil vector")
            nil))))
    (catch Exception e
      (println ">>>>> parse-json-param final exception:" (.getMessage e))
      (throw (ex-info "Malformed or invalid filter input."
                      {:status 400
                       :type :parse-error
                       :cause (.getMessage e)})))))

;; ------------------------------------------------------------
;; Helpers
;; ------------------------------------------------------------

(defn numeric-string?
  [s]
  (boolean (re-matches #"^-?\d+(\.\d+)?$" (str s))))

(defn parse-num [s]
  (if (numeric-string? s)
    (bigdec s)
    s))

(defn safe-trim [s]
  (if (string? s)
    (str/trim s)
    s))

;; ------------------------------------------------------------
;; Operator-specific helpers (all with logs)
;; ------------------------------------------------------------

(defn parse-equality [s]
  (println ">>> parse-equality" s)
  (cond
    (str/starts-with? s "=") {:op := :val (parse-num (subs s 1))}
    (str/starts-with? s "!=") {:op :<> :val (parse-num (subs s 2))}))

(defn parse-comparison [s]
  (println ">>> parse-comparison" s)
  (cond
    (str/starts-with? s ">=") {:op :>= :val (parse-num (subs s 2))}
    (str/starts-with? s "<=") {:op :<= :val (parse-num (subs s 2))}
    (str/starts-with? s ">") {:op :> :val (parse-num (subs s 1))}
    (str/starts-with? s "<") {:op :< :val (parse-num (subs s 1))}))

(defn parse-like [s low]
  (println ">>> parse-like" s)
  (cond
    (str/starts-with? low "not ilike") {:op :not-ilike :val (subs s 9)}
    (str/starts-with? low "ilike") {:op :ilike :val (subs s 5)}
    (str/starts-with? low "not like") {:op :not-like :val (subs s 8)}
    (str/starts-with? low "like") {:op :like :val (subs s 4)}))

(defn parse-in [s low]
  (println ">>> parse-in start, raw s:" s "type:" (type s)
           "| low:" low "type:" (type low))
  (try
    (cond
      (re-matches #"^not +in\[.*\]$" low)
      (let [inner (subs s 7 (dec (count s)))]
        (println ">>> parse-in not-in inner:" inner "type:" (type inner))
        (let [split-result (clojure.string/split inner #",")]
          (println ">>> parse-in not-in split-result:" split-result
                   "type:" (type split-result)
                   "first elem type:" (some-> split-result first type))
          (let [vals (mapv clojure.string/trim split-result)]
            (println ">>> parse-in not-in vals parsed:" vals
                     "| type:" (type vals)
                     "| element types:" (mapv type vals))
            {:op :not-in :val vals})))

      (re-matches #"^in\[.*\]$" low)
      (let [inner (subs s 3 (dec (count s)))]
        (println ">>> parse-in in inner:" inner "type:" (type inner))
        (let [split-result (clojure.string/split inner #",")]
          (println ">>> parse-in in split-result:" split-result
                   "type:" (type split-result)
                   "first elem type:" (some-> split-result first type))
          (let [vals (mapv clojure.string/trim split-result)]
            (println ">>> parse-in in vals parsed:" vals
                     "| type:" (type vals)
                     "| element types:" (mapv type vals))
            {:op :in :val vals}))))
    (catch Exception e
      (println ">>> parse-in exception:" (.getMessage e))
      (println ">>> parse-in exception class:" (type e))
      (throw e))))

(defn parse-null [low]
  (println ">>> parse-null" low)
  (cond
    (= low "isnull") {:op :isnull}
    (= low "not isnull") {:op :not-isnull}))

;; ------------------------------------------------------------
;; Main dispatcher
;; ------------------------------------------------------------

(defn parse-op-value [v]
  (println ">>> parse-op-value called with:" (pr-str v) "type:" (type v))
  (try
    (if (not (string? v))
      (do
        (println ">>> parse-op-value non-string, default :=")
        {:op := :val v})
      (let [s (safe-trim v)
            low (str/lower-case s)]
        (println ">>> parse-op-value trimmed:" s)
        (or
         (parse-equality s)
         (parse-comparison s)
         (parse-like s low)
         (parse-in s low)
         (parse-null low)
         {:op := :val (parse-num s)})))
    (catch Exception e
      (println ">>> parse-op-value exception:" (.getMessage e))
      (throw e))))

;; ------------------------------------------------------------
;; SQL builder
;; ------------------------------------------------------------

(defn add-filter
  [query [k v] raw-filter-keys]
  (let [k-str (name k)
        raw-filter-strs (set (map #(name (keyword %)) raw-filter-keys))
        property-key-str (str "properties_" k-str)
        is-property? (contains? raw-filter-strs property-key-str)

        _ (println ">o> abc.k-str" k-str)

        field (cond
                ;; TODO: special cases for joined tables not yet implemented
                (= k-str "supplier") (keyword "suppliers.name")
                is-property? [:raw (format "items.properties ->> '%s'" property-key-str)]
                :else (keyword (str "items." k-str)))

        {:keys [op val]} (parse-op-value v)
        val (cast-uuid val)]

    (println ">>> add-filter field:" field "value:" v "is-property?:" is-property? "op:" op)

    (case op
      :isnull (sql/where query [:is field nil])
      :not-isnull (sql/where query [:is-not field nil])
      :in (sql/where query [:in field val])
      :not-in (sql/where query [:not-in field val])
      :like (sql/where query [:like field val])
      :not-like (sql/where query [:not [:like field val]])
      :ilike (sql/where query [:ilike field val])
      :not-ilike (sql/where query [:not [:ilike field val]])
      :<> (sql/where query [:<> field val])
      := (sql/where query [:= field val])
      :> (sql/where query [:> field val])
      :< (sql/where query [:< field val])
      :>= (sql/where query [:>= field val])
      :<= (sql/where query [:<= field val])
      query)))

(defn add-filter-groups
  ([base-query groups]
   (add-filter-groups base-query groups nil))
  ([base-query groups raw-filter-keys]
   (let [group-conds
         (for [group groups]
           (-> (reduce
                (fn [q pair]
                  (add-filter q pair raw-filter-keys))
                (sql/select)
                group)
               :where))]
     (if (seq group-conds)
       (sql/where base-query (cons :or group-conds))
       base-query))))

;; ------------------------------------------------------------
;; Whitelist validation
;; ------------------------------------------------------------

(defn normalize-key [k wl-set]
  (let [kname (name k)
        prefixed (keyword (str "properties_" kname))]
    (cond
      (contains? wl-set k) k
      (contains? wl-set prefixed) prefixed
      :else k)))

(defn normalize-group [group wl-set]
  (into {}
        (map (fn [[k v]]
               [(normalize-key k wl-set) v])
             group)))

(defn validate-filters [filter-groups whitelist]
  (let [wl-set (set (map keyword whitelist))]
    (reduce
     (fn [{:keys [valid invalid]} group]
       (let [normalized (normalize-group group wl-set)
             allowed (set/intersection wl-set (set (keys normalized)))
             denied (set/difference (set (keys normalized)) wl-set)]
         {:valid (conj valid (select-keys normalized allowed))
          :invalid (into invalid denied)}))
     {:valid [] :invalid []}
     filter-groups)))