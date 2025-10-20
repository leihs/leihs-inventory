(ns leihs.inventory.server.resources.pool.fields.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.utils.request-utils :refer [query-params]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response]]
   [taoensso.timbre :refer [error spy]]))

(def excluded-keys #{:active :dynamic})
(def data->keys-mapping {:label [:data :label]
                         :group [:data :group]
                         :type [:data :type]})

(defn target-type-expr [ttype]
  (if (= ttype "package")
    [:= [:raw "fields.data->>'forPackage'"] "true"]
    (let [ttype-expr [:raw "fields.data->>'target_type'"]]
      [:or
       [:is-null ttype-expr]
       [:= ttype-expr ttype]])))

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

(defn base-query [ttype]
  (-> (sql/select :*)
      (sql/from :fields)
      (sql/where [:= :fields.active true])
      (sql/where (target-type-expr ttype))))

(defn transform-field-data [field]
  (let [stripped (apply dissoc field excluded-keys)
        mapped (reduce (fn [m [new-key path]]
                         (assoc m new-key (get-in m path)))
                       stripped
                       data->keys-mapping)]
    (-> (merge stripped mapped)
        (dissoc :data))))

(defn index-resources [{:keys [tx] :as request}]
  (try
    (let [{:keys [target_type]} (query-params request)
          query (base-query target_type)
          fields (jdbc/query tx (spy (sql-format query :inline true)))
          transformed-fields (map transform-field-data fields)]
      (response {:fields (vec transformed-fields)}))
    (catch Exception e
      (error "Failed to get fields" e)
      (bad-request {:error "Failed to get fields" :details (.getMessage e)}))))

(comment
 (require '[leihs.core.db :as db])
 (let [tx (db/get-ds)]
   (-> (base-query "item")
       (sql-format :inline true)
       (->> (jdbc/query tx))
       (->> (map transform-field-data)))))
