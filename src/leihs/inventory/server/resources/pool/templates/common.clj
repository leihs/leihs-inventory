(ns leihs.inventory.server.resources.pool.templates.common
  (:require
   [clojure.set :refer [rename-keys]]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.models.common :refer [fetch-thumbnails-for-ids
                                                                model->enrich-with-image-attr]]
   [next.jdbc :as jdbc]
   [taoensso.timbre :refer [debug warn]]))

(def case-condition
  [:and
   [:= :i.is_borrowable true]
   [:is :i.retired nil]
   [:= :i.parent_id nil]])

(def count-quantity-condition (sq/call :count
                                       [[:case case-condition 1 :else nil]]))

(defn template-query [template-id pool-id]
  (-> (sql/select
       :mg.name
       :mg.type
       :m.id
       :m.product
       :m.version
       :m.cover_image_id
       [:m.name :model_name]
       :ml.quantity
       [count-quantity-condition
        :available]
       [[:case
         [:<=
          :ml.quantity
          count-quantity-condition]
         true
         :else false]
        :is_quantity_ok])
      (sql/from [:model_links :ml])
      (sql/join [:models :m] [:= :ml.model_id :m.id])
      (sql/left-join [:items :i]
                     [:and
                      [:= :i.model_id :m.id]
                      [:= :i.inventory_pool_id pool-id]])
      (sql/join [:model_groups :mg] [:= :mg.id :ml.model_group_id])
      (sql/where [:and
                  [:= :ml.model_group_id template-id]
                  [:= :mg.type "Template"]])
      (sql/group-by :mg.name :mg.type :m.id :m.product :m.version :m.cover_image_id :ml.quantity)
      (sql/order-by [:m.product :asc])
      sql-format))

(defn ensure-inventory-pool-template-link-exists!
  [tx pool-id template-id]
  (let [rows (jdbc/execute! tx
                            (-> (sql/select :*)
                                (sql/from :inventory_pools_model_groups)
                                (sql/where
                                 [:and
                                  [:= :inventory_pool_id pool-id]
                                  [:= :model_group_id template-id]])
                                sql-format))]
    (case (count rows)
      0 (jdbc/execute-one! tx
                           (-> (sql/insert-into :inventory_pools_model_groups)
                               (sql/values [{:inventory_pool_id pool-id
                                             :model_group_id template-id}])
                               (sql/returning :*)
                               sql-format))
      1 (first rows)
      (do ((warn (format "Found %d entries for pool-id=%s template-id=%s"
                         (count rows) pool-id template-id)))
          (first rows)))))

(defn process-create-template-models [tx entries-to-insert template-id pool-id]
  (debug "process: create models" entries-to-insert)
  (ensure-inventory-pool-template-link-exists! tx pool-id template-id)

  (doseq [entry entries-to-insert]
    (jdbc/execute-one! tx (-> (sql/insert-into :model_links)
                              (sql/values [{:model_id (:id entry)
                                            :model_group_id template-id
                                            :quantity (:quantity entry)}])
                              sql-format))))

(defn process-update-template-models [tx entries-to-update]
  (debug "process: update models" entries-to-update)
  (doseq [{:keys [id quantity]} entries-to-update]
    (jdbc/execute-one! tx (-> (sql/update [:model_links :ml])
                              (sql/set {:quantity quantity})
                              (sql/where [:= :ml.model_id id])
                              (sql/returning :*)
                              sql-format))))

(defn process-delete-template-models [tx entries-to-delete]
  (debug "process: delete models" entries-to-delete)
  (doseq [{:keys [id]} entries-to-delete]
    (jdbc/execute-one! tx (-> (sql/delete-from :model_links)
                              (sql/where [:= :model_id id])
                              (sql/returning :*)
                              sql-format))))

(defn fetch-template-with-models!
  ([tx template-id pool-id]
   (fetch-template-with-models! tx template-id pool-id true))

  ([tx template-id pool-id process-at-least-one-model-check?]
   (let [templates (jdbc/execute! tx (template-query template-id pool-id))]
     (if (and process-at-least-one-model-check? (empty? templates))
       (do
         (debug "template-id=" template-id ", pool-id=" pool-id)
         (throw (ex-info "Template must have at least one model" {:status 400})))
       (let [result (->> templates
                         (group-by :name)
                         (map (fn [[name records]]
                                {:name name
                                 :models (mapv #(-> (select-keys % [:id :product :version :quantity :available
                                                                    :cover_image_id :is_quantity_ok :model_name])
                                                    (rename-keys {:model_name :name}))
                                               records)}))
                         first)
             models (->> (:models result)
                         (fetch-thumbnails-for-ids tx)
                         (map (model->enrich-with-image-attr pool-id)))]
         (-> result
             (assoc :id template-id :models (vec models))))))))

(defn analyze-datasets
  "Given two collections of maps (db-data and new-data), returns a map with:
     :different-quantity => entries with same :id but different :quantity, merged with DB fields and NEW quantity.
     :missing-in-new-data => entries in db-data whose :id doesn't appear in new-data.
     :new-entries => entries in new-data whose :id doesn't appear in db-data."
  [db-data new-data]
  (let [db-ids (set (map :id db-data))
        new-by-id (into {} (map (juxt :id identity) new-data))]
    {:different-quantity
     (->> db-data
          (keep (fn [{:keys [id quantity] :as db-row}]
                  (when-let [{new-q :quantity} (new-by-id id)]
                    (when (not= quantity new-q)
                      (-> db-row
                          (assoc :previous-quantity quantity)
                          (assoc :quantity new-q))))))
          vec)

     :missing-in-new-data
     (filterv (fn [{:keys [id]}]
                (not (contains? new-by-id id)))
              db-data)

     :new-entries
     (filterv (fn [{:keys [id]}]
                (not (contains? db-ids id)))
              new-data)}))
