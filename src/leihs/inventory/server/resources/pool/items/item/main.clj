(ns leihs.inventory.server.resources.pool.items.item.main
  (:require
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.middlewares.debug :refer [log-by-severity]]
   [leihs.inventory.server.middlewares.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.resources.pool.inventory-code :as inv-code]
   [leihs.inventory.server.resources.pool.items.fields-shared :refer [coerce-field-values
                                                                      in-coercions
                                                                      out-coercions
                                                                      flatten-properties
                                                                      split-item-data
                                                                      validate-field-permissions]]
   [leihs.inventory.server.resources.pool.items.main :refer [assign-items-to-package
                                                             validate-item-ids-for-package]]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request not-found response status]]
   [taoensso.timbre :refer [debug spy]]))

(def ERROR_GET_ITEM "Failed to fetch item")
(def ERROR_UPDATE_ITEM "Failed to update item")

(defn get-one [tx item-id]
  (-> (sql/select :*)
      (sql/from :items)
      (sql/where [:= :items.id item-id])
      sql-format
      (->> (jdbc/execute-one! tx))))

(defn inventory-code-exists? [tx inventory-code exclude-id]
  (let [query (-> (sql/select :id)
                  (sql/from :items)
                  (sql/where [:= :inventory_code inventory-code])
                  (cond-> exclude-id
                    (sql/where [:not= :id exclude-id]))
                  sql-format)]
    (-> (jdbc/execute-one! tx query)
        some?)))

(defn get-child-item-ids [tx parent-id]
  (-> (sql/select :id)
      (sql/from :items)
      (sql/where [:= :parent_id parent-id])
      sql-format
      (->> (jdbc-query tx)
           (map :id))))

(defn remove-items-from-package [tx item-ids]
  (when (seq item-ids)
    (-> (sql/update :items)
        (sql/set {:parent_id nil})
        (sql/where [:in :id item-ids])
        sql-format
        (->> (jdbc/execute! tx)))))

(defn get-resource [{:keys [tx]
                     {{:keys [item_id]} :path} :parameters
                     :as request}]
  (try
    (if-let [item (get-one tx item_id)]
      (let [is-package? (-> (sql/select :is_package)
                            (sql/from :models)
                            (sql/where [:= :id (:model_id item)])
                            sql-format
                            (->> (jdbc/execute-one! tx)
                                 :is_package))
            response-data (-> item
                              flatten-properties
                              (coerce-field-values out-coercions))]
        (response (cond-> response-data
                    is-package?
                    (assoc :item_ids (get-child-item-ids tx item_id)))))
      (not-found {:error "Item not found"}))
    (catch Exception e
      (log-by-severity ERROR_GET_ITEM e)
      (exception-handler request ERROR_GET_ITEM e))))

(defn patch-resource [{:keys [tx]
                       {{:keys [item_id pool_id]} :path
                        update-params :body} :parameters
                       :as request}]
  (try
    (if-let [item (get-one tx item_id)]
      (if-let [validation-error (validate-field-permissions request)]
        (bad-request validation-error)
        (let [item-ids-param (:item_ids update-params)
              {:keys [item-data properties]} (-> update-params
                                                 (dissoc :id :item_ids)
                                                 split-item-data)
              inventory-code (:inventory_code item-data)]
          (when item-ids-param
            (when-not (seq item-ids-param)
              (throw (ex-info "item_ids cannot be empty" {:status 400})))
            (let [invalid-items (validate-item-ids-for-package tx item-ids-param)]
              (when (seq invalid-items)
                (throw (ex-info "Cannot add packages or already assigned items to package"
                                {:status 400 :invalid_item_ids (map :id invalid-items)})))))
          (if (and inventory-code (inventory-code-exists? tx inventory-code item_id))
            (status {:body {:error "Inventory code already exists"
                            :proposed_code (inv-code/propose tx pool_id)}}
                    409)
            (let [item-data-coerced (coerce-field-values item-data in-coercions)
                  properties-json (merge (:properties item) properties)
                  item-data-with-properties (assoc item-data-coerced
                                                   :properties [:lift properties-json])
                  sql-query (-> (sql/update :items)
                                (sql/set item-data-with-properties)
                                (sql/where [:= :id item_id])
                                (sql/returning :*)
                                sql-format)
                  result (jdbc/execute-one! tx sql-query)]
              (when item-ids-param
                (let [current-child-ids (get-child-item-ids tx item_id)
                      to-remove (remove (set item-ids-param) current-child-ids)]
                  (remove-items-from-package tx to-remove)
                  (assign-items-to-package tx item_id item-ids-param)))
              (if result
                (let [is-package? (-> (sql/select :is_package)
                                      (sql/from :models)
                                      (sql/where [:= :id (:model_id result)])
                                      sql-format
                                      (->> (jdbc/execute-one! tx)
                                           :is_package))
                      response-data (-> result
                                        flatten-properties
                                        (coerce-field-values out-coercions))]
                  (response (cond-> response-data
                              is-package?
                              (assoc :item_ids (or item-ids-param
                                                   (get-child-item-ids tx item_id))))))
                (bad-request {:error ERROR_UPDATE_ITEM}))))))
      (not-found {:error "Item not found"}))
    (catch Exception e
      (log-by-severity ERROR_UPDATE_ITEM e)
      (exception-handler request ERROR_UPDATE_ITEM e))))
