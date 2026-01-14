(ns leihs.inventory.server.resources.settings.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.middlewares.debug :refer [log-by-severity]]
   [leihs.inventory.server.middlewares.exception-handler :refer [exception-handler]]
   [next.jdbc.sql :as jdbc]
   [ring.util.response :refer [response]]
   [taoensso.timbre :refer [debug]]))

(def ERROR_GET_USER "Failed to get user")

(def fields [:s.user_image_url
             :s.logo_url
             :s.local_currency_string
             :s.time_zone
             :s.lending_terms_url
             :s.documentation_link])

(defn get-resource [request]
  (try
    (let [tx (:tx request)
          select (apply sql/select fields)
          query (-> select
                    (sql/from [:settings :s]))
          result (jdbc/query tx (sql-format query))]
      (debug (sql-format query :inline true))
      (response (first result)))
    (catch Exception e
      (log-by-severity ERROR_GET_USER e)
      (exception-handler request ERROR_GET_USER e))))
