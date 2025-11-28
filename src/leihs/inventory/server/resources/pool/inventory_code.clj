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
  "Proposes the next available inventory code based on the pool's shortname and latest item."
  [tx pool-id]
  (let [pool (pools/get-by-id tx pool-id), shortname (:shortname pool)]
    (when shortname
      (let [latest-item (-> (sql/select :inventory_code)
                            (sql/from :items)
                            (sql/where [:= :owner_id pool-id])
                            (sql/order-by [:created_at :desc])
                            (sql/limit 1)
                            sql-format
                            (->> (jdbc/query tx))
                            first)
            latest-number (extract-last-number (:inventory_code latest-item))
            next-number (inc latest-number)]
        (str shortname next-number)))))
