(ns leihs.inventory.server.resources.pool.items.filter-handler
  (:require
   [clojure.data.json :as json]
   [clojure.string :as str]
   [clojure.set :as set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]))

(defn parse-json-param [s]
  (try
    (when (seq s)
      (json/read-str s :key-fn keyword))
    (catch Exception _
      (throw (ex-info "Error during json-parse process." {:status 400})))))

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
  "Add a single condition to the base query with type-sensitive handling."
  [query [k v]]
  (cond
    (= k :retired)
    (cond
      (= true v)  (sql/where query [:is-not k nil])
      (= false v) (sql/where query [:is k nil])
      :else       query)

    (between-range? v)    (let [[from to] v]
      (cond-> query
        (and from to) (sql/where [:and [:between k from to]])
        (and (nil? from) to) (sql/where [:and [:<= k to]])
        (and from (nil? to)) (sql/where [:and [:>= k from]])))

    (multi-value? v)    (let [vals (cast-uuid v)]
      (cond-> query
        (seq vals) (sql/where [:and [:in k vals]])))
    :else    (let [val (cast-uuid v)]
      (cond-> query
        (some? val) (sql/where [:and [:= k val]])))))


(defn add-subfilter-group
  "Apply AND inside a single sub-filter group."
  [base-query subfilter]
  (reduce add-filter base-query subfilter))

(defn add-filter-groups
  "Combine multiple subfilter maps with OR between groups."
  [base-query filter-groups]
  (let [group-conds
        (for [group filter-groups]
          (let [tmp-q (add-subfilter-group (sql/select) group)]
            (:where tmp-q)))]
    (reduce
      (fn [q cond-expr]
        (cond-> q cond-expr (sql/where [:or cond-expr])))
      base-query
      group-conds)))

(defn validate-filters
  "Filters out non-whitelisted keys from a vector of filter maps.
   Returns {:valid [...] :invalid [...]}."
  [filter-groups whitelist]
  (let [wl-set (set whitelist)]
    (reduce
      (fn [{:keys [valid invalid]} group]
        (let [group-keys   (set (keys group))
              allowed-keys (set/intersection wl-set group-keys)
              denied-keys  (set/difference group-keys wl-set)
              cleaned      (select-keys group allowed-keys)]
          {:valid   (conj valid cleaned)
           :invalid (into invalid denied-keys)}))
      {:valid [] :invalid []}
      filter-groups)))
