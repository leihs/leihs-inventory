;(ns leihs.inventory.server.resources.pool.user.common
;  (:require [clojure.tools.logging :as log]
;            [honey.sql :refer [format] :rename {format sql-format}]
;            [honey.sql.helpers :as sql]
;            [next.jdbc.sql :as jdbc]))
;
;(defn sql-order-users [sqlmap]
;  (sql/order-by sqlmap [[:concat :users.firstname :users.lastname :users.login :users.id]]))
;
;(def base-sqlmap
;  (-> (sql/select :users.* [[:raw "users.firstname || ' ' || users.lastname"] :name])
;      (sql/from :users)
;      sql-order-users))
;
;(defn get-by-id [tx id]
;  (let [query (-> base-sqlmap (sql/where [:= :id id]) sql-format)]
;    (first (jdbc/query tx query))))
