(ns leihs.inventory.server.resources.pool.inventory-code
  (:require
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.inventory-pools.main :as pools]
   [next.jdbc.sql :as jdbc]))

(defn extract-last-number
  "Extract last number sequence in string"
  [inventory-code]
  (if (nil? inventory-code)
    0
    (let [reversed (str/reverse inventory-code)
          without-leading-non-digits (str/replace reversed #"^[^\d]*" "")
          number-only (str/replace without-leading-non-digits #"[^\d]+.*$" "")
          reversed-back (str/reverse number-only)]
      (if (empty? reversed-back)
        0
        (Integer/parseInt reversed-back)))))

(defn propose
  "Proposes the next available inventory code based on the pool's shortname and highest numeric code."
  [tx pool-id]
  (let [pool (pools/get-by-id tx pool-id), shortname (:shortname pool)]
    (when shortname
      (let [sql-str ["SELECT COALESCE(MAX((NULLIF(regexp_replace(inventory_code, E'\\\\D', '', 'g'), ''))::int), 0) as max_num FROM items WHERE owner_id = ?" pool-id]
            result (-> (jdbc/query tx sql-str) first)
            max-number (:max_num result)
            next-number (inc max-number)]
        (str shortname next-number)))))
