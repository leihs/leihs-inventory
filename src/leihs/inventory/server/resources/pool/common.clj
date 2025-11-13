(ns leihs.inventory.server.resources.pool.common
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.models.basic-coercion :as co]
   [leihs.inventory.server.resources.pool.models.common :refer [filter-and-coerce-by-spec]]
   [next.jdbc :as jdbc]))

(defn str-to-bool
  [s]
  (cond
    (string? s) (case (.toLowerCase s)
                  "true" true
                  "false" false
                  nil)
    :else (boolean s)))

(defn select-entries [tx table columns where-clause]
  (jdbc/execute! tx
                 (-> (apply sql/select columns)
                     (sql/from table)
                     (sql/where where-clause)
                     sql-format)))

(defn fetch-attachments [tx model-id pool-id]
  (let [attachments (->> (select-entries tx :attachments [:id :filename :content_type] [:= :model_id model-id])
                         (map #(assoc % :url (str "/inventory/" pool-id "/models/" model-id "/attachments/" (:id %))
                                      :content_type (:content_type %))))]
    (filter-and-coerce-by-spec attachments ::co/attachment)))

; https://www.postgresql.org/docs/current/errcodes-appendix.html
(def FOREIGN-KEY-VIOLATION-CODE "23503")

(defn is-deletable? [tx table id]
  (jdbc/with-transaction [nested-tx tx {:rollback-only true}]
    (try
      (jdbc/execute-one! nested-tx
                         (sql-format (-> (sql/delete-from table)
                                         (sql/where [:= :id id]))))
      true
      (catch org.postgresql.util.PSQLException e
        (if (= FOREIGN-KEY-VIOLATION-CODE (.getSQLState e))
          false
          (throw e)))
      (catch Exception e
        (throw e)))))

(defn is-option-deletable? [tx option-id]
  (is-deletable? tx :options option-id))
