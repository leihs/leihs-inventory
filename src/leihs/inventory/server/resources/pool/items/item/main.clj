(ns leihs.inventory.server.resources.pool.items.item.main
  (:require
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
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
   [ring.util.response :refer [bad-request response]]
   [taoensso.timbre :refer [debug]]))

(def ERROR_UPDATE_ITEM "Failed to update item")

(defn patch-resource [{:keys [tx] :as request}]
  (try
    (if-let [validation-error (validate-field-permissions request)]
      (bad-request validation-error)
      (let [update-params (body-params request)
            {:keys [item_id]} (path-params request)
            {:keys [item-data properties]} (-> update-params (dissoc :id)
                                               split-item-data)
            item-data-coerced (coerce-field-values item-data in-coercions)
            properties-json (or (not-empty properties) {})
            item-data-with-properties (assoc item-data-coerced
                                             :properties [:lift properties-json])
            sql-query (-> (sql/update :items)
                          (sql/set item-data-with-properties)
                          (sql/where [:= :id item_id])
                          (sql/returning :*)
                          sql-format)
            result (jdbc/execute-one! tx sql-query)]
        (if result
          (response (-> result
                        flatten-properties
                        (coerce-field-values out-coercions)))
          (bad-request {:error ERROR_UPDATE_ITEM}))))
    (catch Exception e
      (log-by-severity ERROR_UPDATE_ITEM e)
      (exception-handler request ERROR_UPDATE_ITEM e))))
