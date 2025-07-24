(ns leihs.inventory.server.resources.pool.options.option.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.core :refer [presence]]
   [leihs.inventory.server.resources.pool.cast-helper :refer [remove-nil-entries-fnc
                                                              double-to-numeric-or-zero
                                                              double-to-numeric-or-nil]]
   [leihs.inventory.server.resources.pool.models.common :refer [fetch-thumbnails-for-ids
                                                                filter-map-by-spec]]
   [leihs.inventory.server.resources.pool.models.model.common-model-form :refer [extract-model-form-data
                                                                                 process-accessories
                                                                                 process-categories
                                                                                 process-compatibles
                                                                                 process-entitlements
                                                                                 process-properties]]

   [leihs.inventory.server.resources.pool.models.queries :refer [base-inventory-query
                                                                 filter-by-type
                                                                 from-category
                                                                 with-items
                                                                 with-search
                                                                 without-items]]

   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.exception-handler :refer [exception-to-response]]
   [leihs.inventory.server.utils.helper :refer [url-ends-with-uuid?]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response
                                                    fetch-pagination-params
                                                    fetch-pagination-params-raw]]
   [leihs.inventory.server.utils.request-utils :refer [path-params
                                                       query-params]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [debug error]])
  (:import
   (java.time LocalDateTime)))

(defn fetch-option-handler-by-pool-form [request]
  (let [current-timestamp (LocalDateTime/now)
        tx (get-in request [:tx])
        option-id (to-uuid (get-in request [:path-params :option_id]))
        pool-id (to-uuid (get-in request [:path-params :pool_id]))]
    (try
      (let [model-query (->
                         (sql/select :o.*)
                         (sql/from [:options :o])
                         (sql/where [:= :o.id option-id])
                         sql-format)
            result (jdbc/execute-one! tx model-query)]
        (if result
          (response result)
          (bad-request {:error "Failed to fetch model"})))
      (catch Exception e
        (error "Failed to fetch model" (.getMessage e))
        (bad-request {:error "Failed to fetch model" :details (.getMessage e)})))))

(defn update-option-handler-by-pool-form [request]
  (let [option-id (to-uuid (get-in request [:path-params :option_id]))
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        multipart (get-in request [:parameters :multipart])
        tx (:tx request)

        price (double-to-numeric-or-nil (:price multipart))
        multipart (assoc multipart :price price)]
    (try
      (let [update-model-query (-> (sql/update :options)
                                   (sql/set multipart)
                                   (sql/where [:= :id option-id])
                                   (sql/returning :*)
                                   sql-format)
            updated-model (jdbc/execute-one! tx update-model-query)]

        (if updated-model
          (response updated-model)
          (bad-request {:error "Failed to update model"})))
      (catch Exception e
        (error "Failed to update model" (.getMessage e))
        (bad-request {:error "Failed to update model" :details (.getMessage e)})))))
