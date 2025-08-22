(ns leihs.inventory.server.resources.pool.fields.main
  (:require
   [clojure.set]
   [honey.sql :refer [call format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.utils.request-utils :refer [path-params
                                                       query-params]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response]]
   [taoensso.timbre :refer [debug error spy]]))

(defn target-type-expr [ttype]
  (let [ttype-expr [:raw "fields.data->>'target_type'"]]
    [:or
     [:is-null ttype-expr]
     [:= ttype-expr (name ttype)]]))

(defn req-owner?-expr [true-or-false]
  [:in
   [:raw "fields.data->'permissions'->>'owner'"]
   (case true-or-false
     true ["true" "false"]
     false ["false"])])

(defn min-req-role-expr [min-req-role]
  [:in
   [:raw "fields.data->'permissions'->>'role'"]
   (case min-req-role
     :lending_manager ["lending_manager"]
     :inventory_manager ["lending_manager" "inventory_manager"])])

(defn base-query [ttype req-owner? min-req-role]
  (-> (sql/select :*)
      (sql/from :fields)
      (sql/where [:= :fields.active true])
      (sql/where (target-type-expr ttype))
      (sql/where (req-owner?-expr req-owner?))
      (sql/where (min-req-role-expr min-req-role))))

(comment
 (require '[leihs.core.db :as db])
 (let [tx (db/get-ds)]
   (-> (base-query :item false :inventory_manager)
       (dissoc :select)
       ; (sql/select :%count.*)
       (sql/select :id)
       (sql-format :inline true)
       (->> (jdbc/query tx))
       )))

(defn index-resources [request]
  (debug request)
  (response {}))
