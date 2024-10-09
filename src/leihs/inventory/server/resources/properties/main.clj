(ns leihs.inventory.server.resources.properties.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [path-params]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]]))

(defn get-properties-handler [request]
  (try
    (let [tx (:tx request)
          query (-> (sql/select :p.*)
                    (sql/from [:properties :p])
                    (sql/limit 10)
                    sql-format)
          result (jdbc/query tx query)]
      (response result))
    (catch Exception e
      (error "Failed to get properties" e)
      (bad-request {:error "Failed to get properties" :details (.getMessage e)}))))