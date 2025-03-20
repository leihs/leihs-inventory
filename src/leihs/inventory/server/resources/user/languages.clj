(ns leihs.inventory.server.resources.user.languages
  (:require [clojure.tools.logging :as log]
            [honey.sql :refer [format] :rename {format sql-format}]
            [honey.sql.helpers :as sql]
            [leihs.inventory.server.resources.user.common :refer [get-by-id]]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :refer [query] :rename {query jdbc-query}]))

(def base-sqlmap (-> (sql/select :languages.*)
                     (sql/from :languages)
                     (sql/where [:= :active true])
                     (sql/order-by [:name :asc])))

(defn get-by-locale [tx locale]
  (-> base-sqlmap
      (sql/where [:= :locale locale])
      sql-format
      (->> (jdbc-query tx))
      first))

(defn default [tx]
  (-> base-sqlmap
      (sql/where [:= :languages.default true])
      sql-format
      (->> (jdbc-query tx))
      first))

(defn get-the-one-to-use [tx user-id]
  (or (some->> user-id
               (get-by-id tx)
               :language_locale
               (get-by-locale tx))
      (default tx)))

(defn one-to-use [tx user-id]
  (get-the-one-to-use tx user-id))

(defn get-one [tx {:keys [language-locale]}]
  (get-by-locale tx language-locale))

(defn get-multiple [tx]
  (-> base-sqlmap sql-format (->> (jdbc-query tx))))
