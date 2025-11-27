(ns leihs.inventory.server.resources.pool.items.item.main
  (:require
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.inventory-code :as inv-code]
   [leihs.inventory.server.resources.pool.items.fields-shared :refer [coerce-field-values
                                                                      in-coercions
                                                                      out-coercions
                                                                      flatten-properties
                                                                      split-item-data
                                                                      validate-field-permissions]]
   [leihs.inventory.server.utils.debug :refer [log-by-severity]]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.request-utils :refer [body-params path-params]]
   [next.jdbc :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [debug spy]]))

(def ERROR_UPDATE_ITEM "Failed to update item")

(defn inventory-code-exists? [tx inventory-code exclude-id]
  (let [query (-> (sql/select :id)
                  (sql/from :items)
                  (sql/where [:= :inventory_code inventory-code])
                  (cond-> exclude-id
                    (sql/where [:not= :id exclude-id]))
                  sql-format)]
    (-> (jdbc/execute-one! tx query)
        some?)))

(defn patch-resource [{:keys [tx] :as request}]
  (try
    (if-let [validation-error (validate-field-permissions request)]
      (bad-request validation-error)
      (let [update-params (body-params request)
            {:keys [item_id pool_id]} (path-params request)
            {:keys [item-data properties]} (-> update-params (dissoc :id)
                                               split-item-data)
            inventory-code (:inventory_code item-data)]
        (if (and inventory-code (inventory-code-exists? tx inventory-code item_id))
          (status {:body {:error "Inventory code already exists"
                          :proposed_code (inv-code/propose tx pool_id)}}
                  409)
          (let [item-data-coerced (coerce-field-values item-data in-coercions)
                properties-json (or (not-empty properties) {})
                item-data-with-properties (cond-> item-data-coerced
                                            (contains? item-data-coerced :properties)
                                            (assoc :properties [:lift properties-json]))
                sql-query (-> (sql/update :items)
                              (sql/set item-data-with-properties)
                              (sql/where [:= :id item_id])
                              (sql/returning :*)
                              (sql-format :inline true))
                result (jdbc/execute-one! tx sql-query)]
            (if result
              (response (-> result
                            flatten-properties
                            (coerce-field-values out-coercions)))
              (bad-request {:error ERROR_UPDATE_ITEM}))))))
    (catch Exception e
      (log-by-severity ERROR_UPDATE_ITEM e)
      (exception-handler request ERROR_UPDATE_ITEM e))))
