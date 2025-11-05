(ns leihs.inventory.server.resources.pool.attachments.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.utils.debug :refer [log-by-severity]]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.request-utils :refer [path-params]]
   [next.jdbc.sql :as jdbc]
   [ring.util.response :refer [header response]]))

(def base-query
  (-> (sql/select :*)
      (sql/from :attachments)))

(defn get-by-item-id [tx item-id]
  (-> base-query
      (sql/where [:= :attachments.item_id item-id])
      sql-format
      (->> (jdbc/query tx))))
