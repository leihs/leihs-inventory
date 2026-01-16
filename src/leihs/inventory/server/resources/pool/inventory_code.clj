(ns leihs.inventory.server.resources.pool.inventory-code
  (:require
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.inventory-pools.main :as pools]
   [next.jdbc.sql :as jdbc]))

(defn- last-number-sql-expression
  "Returns SQL expression to extract last number from inventory_code (mirroring Ruby legacy logic)"
  []
  "CASE 
     WHEN inventory_code ~ '\\\\d' THEN
       (regexp_replace(
         reverse(
           regexp_replace(
             reverse(inventory_code), 
             '^[^\\\\d]*', 
             ''
           )
         ),
         '[^\\\\d]+.*$', 
         ''
       )::int
     ELSE 0 
   END")

(defn extract-last-number
  "Extract last number sequence from inventory code using SQL (mirroring Ruby legacy logic)"
  [tx inventory-code]
  (if (nil? inventory-code)
    0
    (let [sql-str [(str "SELECT " 
                      (str/replace (last-number-sql-expression) "inventory_code" "?") 
                      " as last_num") inventory-code]
          result (-> (jdbc/query tx sql-str) first)
          last-num (:last_num result)]
      last-num)))

(defn propose
  "Proposes the next available inventory code based on the pool's shortname and highest numeric code."
  [tx pool-id]
  (let [pool (pools/get-by-id tx pool-id), shortname (:shortname pool)]
    (when shortname
      (let [sql-str [(str "SELECT COALESCE(MAX(" (last-number-sql-expression) "), 0) as max_num "
                          "FROM items WHERE owner_id = ?") pool-id]
            result (-> (jdbc/query tx sql-str) first)
            max-number (:max_num result)
            next-number (inc max-number)]
        (str shortname next-number)))))
