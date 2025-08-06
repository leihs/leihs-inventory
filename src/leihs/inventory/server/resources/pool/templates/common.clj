(ns leihs.inventory.server.resources.pool.templates.common
  (:require
   [clojure.string :as str]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [next.jdbc :as jdbc]
   [taoensso.timbre :refer [debug error]]))

(defn template-query [template-id pool-id]
  (-> (sql/select
       :mg.name
       :mg.type
       :m.id
       :m.product
       :ml.quantity
       [(sq/call :count
                 [[:case
                   [:and
                    [:= :i.is_borrowable true]
                    [:is :i.retired nil]]
                   1
                   :else nil]])
        :available]
       [[:case
         [:<
          :ml.quantity
          (sq/call :count
                   [[:case
                     [:and
                      [:= :i.is_borrowable true]
                      [:is :i.retired nil]]
                     1
                     :else nil]])]
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
      (sql/group-by :mg.name :mg.type :m.id :m.product :ml.quantity)
      (sql/order-by [:m.product :asc])
      sql-format))

(defn process-create-template-models [tx entries-to-insert template-id pool-id]
  (debug "process: create models" entries-to-insert)
  (doseq [entry entries-to-insert]

      ;; TODO: check if this is also done in legacy
    (jdbc/execute-one! tx (-> (sql/insert-into :inventory_pools_model_groups)
                              (sql/values [{:inventory_pool_id pool-id
                                            :model_group_id template-id}])
                              (sql/returning :*)
                              sql-format))

    (jdbc/execute-one! tx (-> (sql/insert-into :model_links)
                              (sql/values [{:model_id (:id entry)
                                            :model_group_id template-id
                                            :quantity (:quantity entry)}])
                              (sql/returning :*)
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

(defn fetch-template-with-models
  "Generates a template map with the given template-id and pool-id.
   Returns a map with keys :id, :name, and :models."

  ([tx template-id pool-id]
   (fetch-template-with-models tx template-id pool-id true))

  ([tx template-id pool-id process-at-least-one-model-check?]
   (let [templates (jdbc/execute! tx (template-query template-id pool-id))
         _ (when (and process-at-least-one-model-check? (= 0 (count templates)))
             (do
               (debug ">o> abc" template-id pool-id)
               (throw (ex-info "Template must have at least one model" {:status 404}))))
         templates (->> templates
                        (group-by :name)
                        (map (fn [[name records]]
                               {:name name
                                :models (mapv #(select-keys % [:id :product :quantity :available :is_quantity_ok]) records)}))
                        first)
         templates (assoc templates :id template-id)] templates)))

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
