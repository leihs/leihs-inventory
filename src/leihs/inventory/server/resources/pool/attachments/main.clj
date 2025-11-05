(ns leihs.inventory.server.resources.pool.attachments.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [next.jdbc.sql :as jdbc]))

(def base-query
  (-> (sql/select :*)
      (sql/from :attachments)))

(defn get-by-item-id [tx item-id]
  (-> base-query
      (sql/where [:= :attachments.item_id item-id])
      sql-format
      (->> (jdbc/query tx))))
