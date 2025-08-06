(ns leihs.inventory.server.resources.pool.templates.main
  (:require
   [clojure.set]
   [clojure.string :as str]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.models.common :refer [filter-and-coerce-by-spec]]
   [leihs.inventory.server.resources.pool.templates.common :refer [analyze-datasets
                                                                   case-condition
                                                                   fetch-template-with-models
                                                                   process-create-template-models]]
   [leihs.inventory.server.resources.pool.templates.types :as types]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.helper :refer [log-by-severity]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response status]]))

(def ERROR_CREATION "Failed to create template")
(def ERROR_FETCH "Failed to fetch template")

(defn post-resource [request]
  (try
    (let [tx (:tx request)
          pool-id (to-uuid (get-in request [:path-params :pool_id]))
          {:keys [name models]} (get-in request [:parameters :body])
          data {:type "Template" :name name}
          res (jdbc/execute-one! tx (-> (sql/insert-into :model_groups)
                                        (sql/values [data])
                                        (sql/returning :*)
                                        sql-format))
          template-id (:id res)
          template-data (fetch-template-with-models tx template-id pool-id false)
          analyzed-datasets (analyze-datasets (:models template-data) models)
          entries-to-insert (:new-entries analyzed-datasets)]
      (process-create-template-models tx entries-to-insert template-id pool-id)
      (if-let [templates (fetch-template-with-models tx template-id pool-id)]
        (response templates)
        (bad-request {:error ERROR_CREATION})))
    (catch Exception e
      (log-by-severity ERROR_CREATION e)
      (cond
        (str/includes? (.getMessage e) "violates")
        (-> (response {:status "failure"
                       :message ERROR_CREATION
                       :detail (.getMessage e)})
            (status 409))
        :else (bad-request {:error ERROR_CREATION :details (.getMessage e)})))))

(defn template-quantity-ok-query
  [pool-id template-ids]
  (-> (sql/select
       [(keyword "mg.id") :model_group_id]
       [:ml.quantity]
       [[:case
         [:<= :ml.quantity
          [:count
           [:case case-condition
            [:raw "1"]
            :else nil]]]
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
      (sql/join [:inventory_pools_model_groups :ipmg] [:= :ipmg.model_group_id :mg.id])
      (sql/where [:and
                  [:in :ml.model_group_id template-ids]
                  [:= :mg.type "Template"]
                  [:= :ipmg.inventory_pool_id pool-id]])
      (sql/group-by :mg.id :ml.quantity)
      sql-format))

(defn- group-quantity-ok [rows]
  (->> rows
       (group-by :id)
       (mapv (fn [[group-id entries]]
               {:id group-id
                :is_quantity_ok (every? :is_quantity_ok entries)}))))

(defn- merge-by-model-group-id [vec1 vec2]
  (let [indexed (into {} (map (juxt :id identity) vec2))]
    (mapv (fn [m1]
            (merge m1 (get indexed (:id m1))))
          vec1)))

(defn- rename-model-group-id-to-id [rows]
  (mapv (fn [m]
          (-> m
              (assoc :id (:model_group_id m))
              (dissoc :model_group_id)))
        rows))

(defn base-template-query [pool-id] (-> (sql/select [[:count :ml.model_id] :models_count]
                                                    :mg.id
                                                    :mg.name
                                                    :mg.created_at
                                                    :mg.updated_at)
                                        (sql/from [:model_groups :mg])
                                        (sql/join [:inventory_pools_model_groups :ipmg] [:= :mg.id :ipmg.model_group_id])
                                        (sql/left-join [:model_links :ml] [:= :ml.model_group_id :mg.id])
                                        (sql/where [:and
                                                    [:= :ipmg.inventory_pool_id pool-id]
                                                    [:= :mg.type "Template"]])
                                        (sql/group-by :mg.id :mg.name :mg.created_at :mg.updated_at)
                                        (sql/order-by [:mg.name :asc])))

(defn index-resources [request]
  (try
    (let [tx (get-in request [:tx])
          pool-id (to-uuid (get-in request [:path-params :pool_id]))
          post-fnc (fn [models]
                     (if (seq models)
                       (let [template-ids (mapv :id models)
                             query (template-quantity-ok-query pool-id template-ids)
                             res (->> (jdbc/execute! tx query)
                                      (rename-model-group-id-to-id)
                                      (group-quantity-ok)) models (merge-by-model-group-id models res)]
                         (filter-and-coerce-by-spec models ::types/data-keys))
                       models))]

      (response (create-pagination-response request (base-template-query pool-id) nil post-fnc)))
    (catch Exception e
      (log-by-severity ERROR_FETCH e)
      (bad-request {:error ERROR_FETCH :details (.getMessage e)}))))
