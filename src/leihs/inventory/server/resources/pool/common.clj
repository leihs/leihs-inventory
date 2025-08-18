(ns leihs.inventory.server.resources.pool.common
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.models.basic_coercion :as co]
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

(defn is-model-deletable?
  "Returns true when the model has no reservations.
   Throws if mtype is not \"Model\" or \"Software\"."
  [tx model-id mtype]
  (let [allowed-types #{"Model" "Software"}]
    (when-not (contains? allowed-types mtype)
      (throw (ex-info "Invalid model type. Expected \"Model\" or \"Software\"."
                      {:error :invalid-model-type
                       :given mtype
                       :allowed allowed-types})))
    (let [query (-> (sql/select :r.*)
                    (sql/from [:models :m])
                    (sql/right-join [:reservations :r] [:= :m.id :r.model_id])
                    (sql/where [:and
                                [:= :m.id model-id]
                                [:= :m.type mtype]])
                    sql-format)
          result (jdbc/execute! tx query)]
      (empty? result))))

(defn is-option-deletable?
  [tx option-id]
  (let [query (-> (sql/select :r.*)
                  (sql/from [:options :o])
                  (sql/right-join [:reservations :r] [:= :o.id :r.option_id])
                  (sql/where [:= :o.id option-id])
                  sql-format)
        result (jdbc/execute! tx query)]
    (empty? result)))
