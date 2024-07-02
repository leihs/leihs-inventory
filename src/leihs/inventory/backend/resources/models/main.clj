(ns leihs.inventory.backend.resources.models.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [next.jdbc.sql :as jdbc]))

(def models-query
  (-> (sql/select :*)
      (sql/from :models)
      (sql/order-by :models.product)
      (sql/limit 10)))

(defn models [{tx :tx}]
  {:body
   (-> models-query
       sql-format
       (->> (jdbc/query tx)))})

(defn routes [request]
  (case (:request-method request)
    :get (models request)))
