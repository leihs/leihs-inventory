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

(defn- remove-all-nil-entries [rows]
  (remove #(and (nil? (:item_id %))
                (nil? (:reservation_item_id %))
                (nil? (:reservation_model_id %))
                (nil? (:procurement_request_model_id %))
                (nil? (:compatible_model_id %))
                (nil? (:compatible_id %))
                (nil? (:procurement_templates_model_id %)))
          rows))

(defn is-model-deletable?
  "Returns true when the model has no reservations.
   Throws if mtype is not \"Model\" or \"Software\"."
  [tx model-id mtype]
  (let [allowed-types #{"Model" "Software"}]
    (if (contains? allowed-types mtype)
      (let [query (-> (sql/select [:i.id :item_id] [:r.item_id :reservation_item_id] [:r.model_id :reservation_model_id]
                                  [:pr.model_id :procurement_request_model_id]
                                  [:mc1.model_id :compatible_model_id]
                                  [:mc2.compatible_id :compatible_id]
                                  [:pt.model_id :procurement_templates_model_id])

                      (sql/from [:models :m])
                      (sql/left-join [:items :i] [:= :i.model_id :m.id])
                      (sql/left-join [:reservations :r]
                                     [:or
                                      [:= :r.item_id :i.id]
                                      [:= :r.model_id :m.id]])
                      (sql/left-join [:procurement_requests :pr]
                                     [:= :pr.model_id :m.id])
                      (sql/left-join [:models_compatibles :mc1]
                                     [:= :mc1.model_id :m.id])
                      (sql/left-join [:models_compatibles :mc2]
                                     [:= :mc2.compatible_id :m.id])
                      (sql/left-join [:procurement_templates :pt]
                                     [:= :pt.model_id :m.id])
                      (sql/where [:and
                                  [:= :m.id model-id]
                                  [:= :m.type mtype]])
                      sql-format)
            result (jdbc/execute! tx query)
            is-deletable? (empty? (remove-all-nil-entries result))]
        is-deletable?)

      (throw (ex-info "Invalid model type. Expected \"Model\" or \"Software\"."
                      {:error :invalid-model-type
                       :given mtype
                       :allowed allowed-types})))))

(defn is-option-deletable?
  [tx option-id]
  (let [query (-> (sql/select :r.*)
                  (sql/from [:options :o])
                  (sql/right-join [:reservations :r] [:= :o.id :r.option_id])
                  (sql/where [:= :o.id option-id])
                  sql-format)
        result (jdbc/execute! tx query)]
    (empty? result)))
