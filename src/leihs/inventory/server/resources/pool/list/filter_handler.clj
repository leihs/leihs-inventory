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

;(defn parse-json-param [s]
;  (println ">o> filter.s" s)
;  (try
;    (let [trimmed (str/trim (str s))
;          parsed (try
;                   ;; first try JSON
;                   (json/parse-string trimmed true)
;                   (catch JsonParseException _
;                     ;; fallback: EDN (for raw Clojure-style vectors)
;                     (edn/read-string trimmed)))]
;      (cond
;        (and (vector? parsed) (every? map? parsed))
;        parsed
;
;        (map? parsed)
;        [parsed] ;; allow single map
;
;        :else
;        (throw (ex-info "Invalid filter format: must be a vector of maps"
;                 {:status 400 :type :invalid-structure}))))
;    (catch Exception e
;      (throw (ex-info "Malformed or invalid filter input."
;               {:status 400
;                :type :parse-error
;                :cause (.getMessage e)})))))
;
;(defn parse-json-param [s]
;  (println ">o> filter.s" s)
;  (try
;    (let [trimmed (-> s str str/trim)
;          ;; Handle when middleware already decoded JSON to EDN (vector/map)
;          parsed (cond
;                   (vector? s) s
;                   (map? s) [s]
;
;                   ;; Try strict JSON
;                   :else (try
;                           (json/parse-string trimmed true)
;                           (catch Exception _
;                             ;; fallback: tolerate EDN or missing quotes
;                             (edn/read-string trimmed))))]
;      (cond
;        ;; ✅ valid vector of maps
;        (and (vector? parsed) (every? map? parsed))
;        parsed
;
;        ;; ✅ single map: wrap
;        (map? parsed)
;        [parsed]
;
;        :else
;        (throw (ex-info "Invalid filter format: must be a vector of maps"
;                 {:status 400 :type :invalid-structure}))))
;    (catch Exception e
;      (println ">x> filter parse error:" (.getMessage e))
;      (throw (ex-info "Malformed or invalid filter input."
;               {:status 400
;                :type :parse-error
;                :cause (.getMessage e)})))))

(defn parse-json-param [s]
  (println ">>>>> parse-json-param: raw input type:" (type s))
  (println ">>>>> parse-json-param: raw input value:" s)
  (try
    (let [trimmed (-> s str str/trim)]
      (println ">>>>> trimmed:" trimmed)
      (let [parsed
            (cond
              ;; already a Clojure value (Ring or middleware decoded JSON)
              (vector? s) (do (println ">>>>> detected vector input") s)
              (map? s)    (do (println ">>>>> detected map input") [s])

              :else
              (do
                (println ">>>>> trying JSON parse")
                (try
                  (let [res (json/parse-string trimmed true)]
                    (println ">>>>> JSON parsed OK:" (pr-str res))
                    res)
                  (catch Exception e-json
                    (println ">>>>> JSON parse failed:" (.getMessage e-json))
                    (println ">>>>> trying EDN parse fallback")
                    (try
                      (let [res (edn/read-string trimmed)]
                        (println ">>>>> EDN parsed OK:" (pr-str res))
                        res)
                      (catch Exception e-edn
                        (println ">>>>> EDN parse failed:" (.getMessage e-edn))
                        (throw e-edn)))))))]
        (println ">>>>> final parsed value:" (pr-str parsed))
        (cond
          (and (vector? parsed) (every? map? parsed))
          parsed

          (map? parsed)
          [parsed]

          :else
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

(defn add-and-group [base-query subfilter]
  (reduce (fn [q pair] (add-filter q pair))
    base-query
    subfilter))

;(defn add-filter-groups [base-query groups]
;  (let [group-conds
;        (for [group groups]
;          (-> (add-and-group (sql/select) group)
;            :where))]
;    (if (seq group-conds)
;      (sql/where base-query (cons :or group-conds))
;      base-query)))

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
