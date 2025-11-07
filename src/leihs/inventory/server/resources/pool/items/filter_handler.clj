(ns leihs.inventory.server.resources.pool.items.filter-handler
  (:require
   ;[clojure.data.json :as json]
   [cheshire.core :as json]
   [clojure.edn :as edn]
   ;[cheshire.core :as json])

   [clojure.string :as str]
   [clojure.set :as set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql])
  (:import [com.fasterxml.jackson.core JsonParseException]))

(defn parse-json-param
  "Parses a string into Clojure data.
   - Accepts either JSON or EDN.
   - Always returns a vector.
   - Returns [] for nil or empty strings.
   - Throws ex-info {:status 400} on malformed input."
  [s]
  (try
    (let [result
          (cond
            (nil? s) []
            (string? s)
            (let [trimmed (str/trim s)]
              (cond
                (empty? trimmed)
                []

                :else
                (try
                  ;; Try JSON first
                  (json/parse-string trimmed true)
                  (catch JsonParseException _
                    ;; Fallback to EDN
                    (edn/read-string trimmed)))))

            :else s)]

      ;; Always return a vector
      (cond
        (vector? result) result
        (seq? result) (vec result)
        (map? result) [result]
        (nil? result) []
        :else [result]))

    (catch Exception e
      (throw (ex-info "Malformed JSON/EDN input."
               {:status 400
                :type :parse-error
                :cause (.getMessage e)})))))

(defn date-string? [s]
  (and (string? s)
    (re-matches #"\d{4}-\d{2}-\d{2}" s)))

(defn between-range? [v]
  (and (vector? v)
    (= 2 (count v))
    (or (every? number? v)
      (every? date-string? v))))

(defn multi-value? [v]
  (and (vector? v)
    (or (> (count v) 2)
      (and (= 2 (count v))
        (not (between-range? v))))))

(defn uuid-string?
  "Returns true if v looks like a UUID string."
  [v]
  (and (string? v)
    (re-matches
      #"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
      v)))

(defn cast-uuid
  "Casts a value or a sequence of values to UUID type."
  [v]
  (cond
    (vector? v) (mapv #(if (uuid-string? %) [:cast % :uuid] %) v)
    (uuid-string? v) [:cast v :uuid]
    :else v))

(defn add-filter
  [query [k v] raw-filter-keys]
  (let [k-str (name k)
        ;; normalize all raw-filter-keys to strings for comparison
        raw-filter-strs (set (map #(name (keyword %)) raw-filter-keys))
        property-key-str (str "properties_" k-str)
        is-property? (contains? raw-filter-strs property-key-str)]

    (println ">o> abc.raw-filter-keys" raw-filter-strs)
    (println ">o> abc.property-key" property-key-str)
    (println ">o> abc.is-property?" is-property?)

    (let [;; choose SQL field accordingly
          field (if is-property?
                  [:raw (format "i.properties ->> '%s'" property-key-str)]
                  ;[:raw (format "i.properties ->> '%s'" k-str)]
                  k)]
      (cond
        ;; retired flag
        (= k :retired)
        (cond
          (= true v)  (sql/where query [:is-not k nil])
          (= false v) (sql/where query [:is k nil])
          :else query)

        ;; between range (dates, numbers)
        (between-range? v)
        (let [[from to] v]
          (cond-> query
            (and from to) (sql/where [:between field from to])
            (and (nil? from) to) (sql/where [:<= field to])
            (and from (nil? to)) (sql/where [:>= field from])))

        ;; multi-value (arrays, sets)
        (multi-value? v)
        (let [vals (cast-uuid v)]
          (cond-> query
            (seq vals) (sql/where [:in field vals])))

        ;; string-based search
        (string? v)
        (let [pattern (str "%" v "%")]
          (sql/where query [:ilike field pattern]))

        ;; default case
        :else
        (let [val (cast-uuid v)]
          (cond-> query
            (some? val) (sql/where [:= field val])))))))




(defn add-subfilter-group
  "Apply AND inside a single sub-filter group."
  [base-query subfilter raw-filter-keys]
  (reduce (fn [q pair]
            (add-filter q pair raw-filter-keys))
    base-query
    subfilter))



(defn add-filter-groups
  "Combine multiple subfilter maps with OR between groups."
  [base-query filter-groups raw-filter-keys]
  (let [group-conds
        (for [group filter-groups]
          (let [tmp-q (add-subfilter-group (sql/select) group raw-filter-keys)]
            (:where tmp-q)))]
    (if (seq group-conds)
      (sql/where base-query
        (cons :or group-conds))
      base-query)))



(defn validate-filters
  "Validates a vector of filter maps against a whitelist of allowed keys.
   - Filters out non-whitelisted keys.
   - Returns {:valid [...], :invalid [...]}.
   - Logs debug info for each filter group."
  [filter-groups whitelist]
  (let [wl-set (set (map keyword whitelist))]
    (reduce
      (fn [{:keys [valid invalid]} group]
        (let [group-keys   (set (keys group))
              allowed-keys (set/intersection wl-set group-keys)
              denied-keys  (set/difference group-keys wl-set)
              cleaned      (select-keys group allowed-keys)]

          ;; --- Debug logging ---
          (println ">o> validate-filters.group" group)
          (println ">o> allowed-keys:" allowed-keys)
          (println ">o> denied-keys:" denied-keys)
          (println ">o> cleaned:" cleaned)

          {:valid   (conj valid cleaned)
           :invalid (into invalid denied-keys)}))
      {:valid [] :invalid []}
      filter-groups)))