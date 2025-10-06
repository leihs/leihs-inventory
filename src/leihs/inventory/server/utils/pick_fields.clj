(ns leihs.inventory.server.utils.pick-fields
  (:require
   [clojure.string :as str]
   [ring.util.codec :as codec]
   [taoensso.timbre :refer [debug]]))

(defn- as-str [x]
  (cond
    (keyword? x) (name x)
    (symbol? x) (str x)
    (string? x) x
    (map? x) (pr-str x)
    (sequential? x) (pr-str x)
    :else (pr-str x)))

(defn- base [s]
  (let [parts (str/split s #"\.")]
    (last parts)))

(defn- name-or-alias [item]
  (cond
    ;; [expr alias]
    (and (vector? item) (= 2 (count item)))
    (as-str (second item))

    ;; ;; [expr :as alias] or longer vectors
    ;; (and (vector? item) (> (count item) 2))
    ;; (let [v (vec item)
    ;;       idx (.indexOf v :as)]
    ;;   (if (pos? idx)
    ;;     (as-str (nth v (inc idx)))
    ;;     (base (as-str (first v)))))
    ;;
    ;; ;; map like {:alias expr}
    ;; (and (map? item) (= 1 (count item)))
    ;; (let [[k _] (first item)]
    ;;   (as-str k))

    ;; plain expr
    :else
    (base (as-str item))))

(defn- collect-select-lists
  "Return a sequence of all :select vectors found anywhere in the query map."
  [q]
  (let [out (transient [])]
    (letfn [(walk [node]
              (cond
                (map? node)
                (do
                  (when-let [sel (:select node)]
                    (conj! out sel))
                  (doseq [v (vals node)] (walk v)))

                (sequential? node)
                (doseq [v node] (walk v))

                :else nil))]
      (walk q)
      (persistent! out))))

(defn valid-fields-from-query
  "Return vector of unique names (alias or expr base) from all :selects in query-map.
   Skip HoneySQL operator/select-placeholder items like :%count.* (item includes %)."
  [query-map]
  (let [select-lists (collect-select-lists query-map)
        seen (atom #{})
        out (atom [])]
    (doseq [sel select-lists
            item sel]
      (let [raw (name-or-alias item)]
        ;; skip operator-like items e.g. \":%count.*\" or empty names
        (when
         (and raw
              (not (= raw "*"))
              (not (str/blank? raw)))
          (let [n (base raw)]
            (when (and n (not (contains? @seen n)))
              (swap! seen conj n)
              (swap! out conj n))))))

    (when (some #{"*"} @out)
      (throw (IllegalArgumentException.
              "select wildcard '*' is not allowed when using pick fields")))
    @out))

(defn parse-fields
  "Parses a URL-encoded query parameter like 'fields=field1%2Cfield2%2Cfield3' and returns a vector of fields."
  [fields-param]
  (when (and fields-param (not (clojure.string/blank? fields-param)))
    (-> fields-param
        codec/url-decode ; Decode the URL-encoded string
        (clojure.string/split #",") ; Split by commas
        vec))) ; Convert to a vector

(defn pick-fields
  "xs: vector of maps, or a map containing :data (with optional :pagination etc.).
   fields: vector of strings or keywords, e.g. [\"is_package\" \"name\"].
   Returns the same structure but each item map only contains the wanted fields."
  [xs fields query]
  (let [parsed-fields (parse-fields fields)
        ks (set (map #(if (keyword? %) % (keyword %))
                     parsed-fields))
        pick (fn [m] (select-keys m ks))
        valid-fields (valid-fields-from-query query)
        process (fn [x]
                  (cond
                    ;; wrapped form: {:data [...] ...}
                    (and (map? x) (vector? (:data x)))
                    (update x :data #(mapv pick %))

                    ;; direct item map
                    (map? x) (pick x)

                    :else x))] ; unexpected case: leave unchanged

    (debug "pick-fields: requested fields:" parsed-fields "valid fields:" valid-fields)
    (when (seq ks)
      (let [invalid-fields (remove #(some #{%} valid-fields) parsed-fields)]
        (if (seq invalid-fields)
          (throw (IllegalArgumentException.
                  (str "Invalid fields requested: "
                       (str/join ", " invalid-fields))))

          (if (seq fields)
            (cond
              (map? xs) (process xs)
              (sequential? xs) (mapv process xs)
              :else xs)
            xs))))))
