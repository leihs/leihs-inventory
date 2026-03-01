(ns leihs.inventory.server.resources.pool.items.item.main
  (:require
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.middlewares.debug :refer [log-by-severity]]
   [leihs.inventory.server.middlewares.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.resources.pool.fields.main :as fields]
   [leihs.inventory.server.resources.pool.inventory-code :as inv-code]
   [leihs.inventory.server.resources.pool.inventory-pools.main :as pools]
   [leihs.inventory.server.resources.pool.items.fields-shared :refer [coerce-field-values
                                                                      in-coercions
                                                                      out-coercions
                                                                      flatten-properties
                                                                      split-item-data
                                                                      validate-field-permissions
                                                                      validate-retired-reason-requires-retired!]]
   [leihs.inventory.server.resources.pool.items.main :refer [assign-items-to-package]]
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

(defn model-is-package? [tx model-id]
  (-> (sql/select :is_package)
      (sql/from :models)
      (sql/where [:= :id model-id])
      sql-format
      (->> (jdbc/execute-one! tx)
           :is_package)))

(defn get-resource [{:keys [tx]
                     {{:keys [item_id pool_id]} :path} :parameters
                     {:keys [role] user-id :id} :authenticated-entity
                     :as request}]
  (try
    (if-let [item (get-one tx item_id)]
      (let [is-package? (model-is-package? tx (:model_id item))
            item-data (flatten-properties item)
            response-data (coerce-field-values item-data out-coercions)
            pool (pools/get-by-id tx pool_id)
            item-fields (fields/get-fields tx pool user-id role "item" item-data)]
        (response (-> response-data
                      (assoc :fields item-fields)
                      (cond-> is-package?
                        (assoc :item_ids (get-child-item-ids tx item_id))))))
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
              inventory-code (:inventory_code item-data)
              merged-item-data (merge (select-keys item [:retired :retired_reason]) item-data)]
          (validate-retired-reason-requires-retired! merged-item-data)
          (if (and inventory-code (inventory-code-exists? tx inventory-code item_id))
            (status {:body {:error "Inventory code already exists"
                            :proposed_code (inv-code/propose tx pool_id (model-is-package? tx (:model_id item)))}}
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
              (when (some? item-ids-param)
                (let [current-child-ids (get-child-item-ids tx item_id)
                      to-remove (remove (set item-ids-param) current-child-ids)]
                  (remove-items-from-package tx to-remove)
                  (when (seq item-ids-param)
                    (assign-items-to-package tx item_id item-ids-param))))
              (if result
                (let [is-package? (model-is-package? tx (:model_id result))
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
