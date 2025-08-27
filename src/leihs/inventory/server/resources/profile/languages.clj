(ns leihs.inventory.server.resources.profile.languages
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]))

(def base-sqlmap (-> (sql/select :languages.*)
                     (sql/from :languages)
                     (sql/where [:= :active true])
                     (sql/order-by [:name :asc])))

(defn get-multiple [tx]
  (-> base-sqlmap sql-format (->> (jdbc-query tx))))
