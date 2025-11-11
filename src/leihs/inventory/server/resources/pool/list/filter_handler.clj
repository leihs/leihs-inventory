(ns leihs.inventory.server.resources.pool.list.filter-handler
  (:require
   [cheshire.core :as json]
   [clojure.edn :as edn]
   [clojure.set :as set]
   [clojure.string :as str]
   [honey.sql.helpers :as sql])
  (:import [com.fasterxml.jackson.core JsonParseException]))

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


;; ------------------------------------------------------------
;; Helpers
;; ------------------------------------------------------------

(defn uuid-string? [v]
  (and (string? v)
    (re-matches
      #"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
      v)))

(defn cast-uuid [v]
  (if (uuid-string? v) [:cast v :uuid] v))

;; ------------------------------------------------------------
;; Operator parser
;; ------------------------------------------------------------

(defn parse-op-value [v]
  (if-not (string? v)
    {:op := :val v}
    (let [s (str/trim v)
          low (str/lower-case s)]
      (cond
        (str/starts-with? s "=")  {:op := :val (subs s 1)}
        (str/starts-with? s "!=") {:op :<> :val (subs s 2)}
        (str/starts-with? s ">=") {:op :>= :val (subs s 2)}
        (str/starts-with? s "<=") {:op :<= :val (subs s 2)}
        (str/starts-with? s ">")  {:op :>  :val (subs s 1)}
        (str/starts-with? s "<")  {:op :<  :val (subs s 1)}

        (str/starts-with? low "not ilike") {:op :not-ilike :val (subs s 9)}
        (str/starts-with? low "ilike")     {:op :ilike     :val (subs s 5)}
        (str/starts-with? low "not like")  {:op :not-like  :val (subs s 8)}
        (str/starts-with? low "like")      {:op :like      :val (subs s 4)}

        (re-matches #"^not +in\[.*\]$" low)
        (let [vals (-> s (subs 7 (dec (count s))) (str/split #",") (map str/trim))]
          {:op :not-in :val vals})
        (re-matches #"^in\[.*\]$" low)
        (let [vals (-> s (subs 3 (dec (count s))) (str/split #",") (map str/trim))]
          {:op :in :val vals})

        (= low "isnull")     {:op :isnull}
        (= low "not isnull") {:op :not-isnull}

        :else {:op := :val s}))))

;; ------------------------------------------------------------
;; SQL builder
;; ------------------------------------------------------------

(defn add-filter [query [k v]]
  (let [field (keyword (str "items." (name k)))
        {:keys [op val]} (parse-op-value v)
        val (cast-uuid val)]
    (case op
      :isnull       (sql/where query [:is field nil])
      :not-isnull   (sql/where query [:is-not field nil])
      :in           (sql/where query [:in field val])
      :not-in       (sql/where query [:not-in field val])
      :like         (sql/where query [:like field val])
      :not-like     (sql/where query [:not [:like field val]])
      :ilike        (sql/where query [:ilike field val])
      :not-ilike    (sql/where query [:not [:ilike field val]])
      :<>           (sql/where query [:<> field val])
      :=            (sql/where query [:= field val])
      :>            (sql/where query [:> field val])
      :<            (sql/where query [:< field val])
      :>=           (sql/where query [:>= field val])
      :<=           (sql/where query [:<= field val])
      query)))


(defn add-filter
  [query [k v] raw-filter-keys]
  (let [k-str (name k)
        ;; normalize all raw-filter-keys to strings for comparison
        raw-filter-strs (set (map #(name (keyword %)) raw-filter-keys))
        property-key-str (str "properties_" k-str)
        is-property? (contains? raw-filter-strs property-key-str)

        ;; choose SQL field accordingly
        field (if is-property?
                [:raw (format "items.properties ->> '%s'" property-key-str)]
                (keyword (str "items." k-str)))

        {:keys [op val]} (parse-op-value v)
        val (cast-uuid val)]

    (println ">>> add-filter field:" field "value:" v "is-property?:" is-property? "op:" op)

    (case op
      :isnull       (sql/where query [:is field nil])
      :not-isnull   (sql/where query [:is-not field nil])
      :in           (sql/where query [:in field val])
      :not-in       (sql/where query [:not-in field val])
      :like         (sql/where query [:like field val])
      :not-like     (sql/where query [:not [:like field val]])
      :ilike        (sql/where query [:ilike field val])
      :not-ilike    (sql/where query [:not [:ilike field val]])
      :<>           (sql/where query [:<> field val])
      :=            (sql/where query [:= field val])
      :>            (sql/where query [:> field val])
      :<            (sql/where query [:< field val])
      :>=           (sql/where query [:>= field val])
      :<=           (sql/where query [:<= field val])
      query)))


(defn add-and-group [base-query subfilter]
  (reduce (fn [q pair] (add-filter q pair))
    base-query
    subfilter))

(defn add-filter-groups
  "Adds all filter groups (vector of maps). The optional third argument
   raw-filter-keys is ignored for now, but kept for call compatibility."
  ([base-query groups]
   (add-filter-groups base-query groups nil))
  ([base-query groups _raw-filter-keys]
   (let [group-conds
         (for [group groups]
           (-> (add-and-group (sql/select) group)
             :where))]
     (if (seq group-conds)
       (sql/where base-query (cons :or group-conds))
       base-query))))


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

(defn validate-filters [filter-groups whitelist]
  (println ">o> filters" filter-groups)
  (let [wl-set (set (map keyword whitelist))]
    (reduce
      (fn [{:keys [valid invalid]} group]
        (let [allowed (set/intersection wl-set (set (keys group)))
              denied  (set/difference (set (keys group)) wl-set)]
          {:valid (conj valid (select-keys group allowed))
           :invalid (into invalid denied)}))
      {:valid [] :invalid []}
      filter-groups)))
