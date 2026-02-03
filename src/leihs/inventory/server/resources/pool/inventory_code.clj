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

(defn propose
  "Proposes next inventory code. Fetches latest 1000 items matching shortname prefix,
   extracts numbers (incl floats), returns shortname + (max+1). For packages, adds P- prefix."
  ([tx pool-id]
   (propose tx pool-id false))
  ([tx pool-id is-package]
   (let [pool (pools/get-by-id tx pool-id)
         shortname (:shortname pool)]
     (when shortname
       (let [query (-> (sql/select :inventory_code)
                       (sql/from :items)
                       (sql/where [:= :owner_id pool-id]
                                  [:ilike :inventory_code (str shortname "%")])
                       (sql/order-by [:created_at :desc])
                       (sql/limit 1000))
             results (jdbc/query tx (sql-format query))
             max-number (->> results
                             (map :inventory_code)
                             (map extract-last-number)
                             (apply max 0))
             next-number (inc max-number)
             base-code (str shortname next-number)]
         (if is-package
           (str "P-" base-code)
           base-code))))))

(comment
  (require '[leihs.core.db :as db])
  (let [tx (db/get-ds)
        pool-id #uuid "8bd16d45-056d-5590-bc7f-12849f034351"]
    (propose tx pool-id)))
