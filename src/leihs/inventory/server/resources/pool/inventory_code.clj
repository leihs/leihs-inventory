(ns leihs.inventory.server.resources.pool.inventory-code
  (:require
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.inventory-pools.main :as pools]
   [next.jdbc.sql :as jdbc]
   [taoensso.timbre :as timbre :refer [spy]]))

(defn extract-last-number
  "Extract last number from inventory code, supporting floats. Finds all numbers, chooses last, rounds up."
  [inventory-code]
  (if (nil? inventory-code)
    0
    (let [number-pattern #"\d+\.?\d*"
          matches (re-seq number-pattern inventory-code)]
      (if (empty? matches)
        0
        (let [last-match (last matches)
              number (Double/parseDouble last-match)]
          (int (Math/ceil number)))))))

(defn- strip-pool-prefix
  "Strip shortname (and optional P- package prefix) from code before number extraction.
   Needed when shortname is purely numeric to avoid blending prefix digits with suffix."
  [code shortname]
  (let [pkg-prefix (str "P-" shortname)]
    (cond
      (str/starts-with? code pkg-prefix) (subs code (count pkg-prefix))
      (str/starts-with? code shortname) (subs code (count shortname))
      :else code)))

(def ^:private FREE_CHECK_BATCH_SIZE 50)
(def ^:private FREE_CHECK_MAX_BATCHES 20)

(defn- same-shortname-pool-ids [shortname]
  (-> (sql/select :id)
      (sql/from :inventory_pools)
      (sql/where [:= [:lower :shortname] (str/lower-case shortname)])))

(defn- same-shortname-codes-query [shortname]
  (-> (sql/select :inventory_code)
      (sql/from :items)
      (sql/where [:in :owner_id (same-shortname-pool-ids shortname)])))

(defn- code-number [shortname code]
  (-> (str code)
      (strip-pool-prefix shortname)
      extract-last-number))

(defn- start-number
  "Number of the newest code of all pools sharing the shortname, at least 1."
  [tx shortname]
  (->> (-> (same-shortname-codes-query shortname)
           (sql/order-by [:created_at :desc])
           (sql/limit 1)
           sql-format)
       (jdbc/query tx)
       first
       :inventory_code
       (code-number shortname)
       (max 1)))

(defn- used-numbers
  "Numbers of all codes of pools sharing the shortname."
  [tx shortname]
  (->> (jdbc/query tx (sql-format (same-shortname-codes-query shortname)))
       (map #(code-number shortname (:inventory_code %)))
       set))

(defn- codes-for-number [shortname n]
  [(str shortname n) (str "P-" shortname n)])

(defn- taken-codes
  "Lower-cased codes among `codes` already used by any item."
  [tx codes]
  (->> (-> (sql/select [[:lower :inventory_code] :code])
           (sql/from :items)
           (sql/where [:in [:lower :inventory_code] (map str/lower-case codes)])
           sql-format)
       (jdbc/query tx)
       (map :code)
       set))

(defn- free-numbers
  "n numbers from `start` on, not in `skip`, whose item and package codes are
   unused in all pools. Checks in batches; falls back to unchecked numbers
   after max batches."
  [tx shortname start n skip]
  (loop [from start
         acc []
         batches 0]
    (let [missing (- n (count acc))]
      (cond
        (zero? missing) acc
        (>= batches FREE_CHECK_MAX_BATCHES) (->> (iterate inc from)
                                                 (remove skip)
                                                 (take missing)
                                                 (into acc))
        :else
        (let [size (max FREE_CHECK_BATCH_SIZE missing)
              candidates (remove skip (range from (+ from size)))
              taken (->> candidates
                         (mapcat #(codes-for-number shortname %))
                         (taken-codes tx))
              free (remove (fn [num]
                             (some #(taken (str/lower-case %))
                                   (codes-for-number shortname num)))
                           candidates)]
          (recur (+ from size)
                 (into acc (take missing free))
                 (inc batches)))))))

(defn propose-many
  "Proposes n free inventory codes, starting at the newest code of all pools
   sharing the shortname and skipping their used numbers. Same as legacy
   'last used +1'. For packages, adds P- prefix."
  [tx pool-id n is-package]
  (when-let [shortname (:shortname (pools/get-by-id tx pool-id))]
    (->> (used-numbers tx shortname)
         (free-numbers tx shortname (start-number tx shortname) n)
         (map #(str (when is-package "P-") shortname %)))))

(defn propose
  "Proposes next free inventory code."
  ([tx pool-id]
   (propose tx pool-id false))
  ([tx pool-id is-package]
   (first (propose-many tx pool-id 1 is-package))))

(defn inventory-code-exists?
  "Checks case-insensitively whether an item other than `exclude-id` uses the code."
  [tx inventory-code exclude-id]
  (let [query (-> (sql/select :id)
                  (sql/from :items)
                  (sql/where [:= [:lower :inventory_code] [:lower inventory-code]])
                  (cond-> exclude-id
                    (sql/where [:not= :id exclude-id]))
                  (sql/limit 1)
                  sql-format)]
    (boolean (seq (jdbc/query tx query)))))

(comment
  (require '[leihs.core.db :as db])
  (let [tx (db/get-ds)
        pool-id #uuid "8bd16d45-056d-5590-bc7f-12849f034351"]
    (propose tx pool-id)))
