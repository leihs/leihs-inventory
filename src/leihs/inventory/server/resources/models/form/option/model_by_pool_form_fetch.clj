(ns leihs.inventory.server.resources.models.form.option.model-by-pool-form-fetch
  (:require
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [honey.sql :as sq :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.models.queries :refer [accessories-query attachments-query
                                                            entitlements-query item-query
                                                            model-links-query properties-query]]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.core :refer [single-entity-get-request?]]
   [leihs.inventory.server.utils.helper :refer [convert-map-if-exist]]
   [leihs.inventory.server.utils.pagination :refer [fetch-pagination-params pagination-response create-pagination-response]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response]]
   [taoensso.timbre :refer [error]])
  (:import [java.time LocalDateTime]
           [java.util UUID]))

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
            model-result (jdbc/execute-one! tx model-query)

            result (if model-result
                     [model-result]
                     [])]
        (if result
          (response result)
          (bad-request {:error "Failed to fetch model"})))
      (catch Exception e
        (error "Failed to fetch model" (.getMessage e))
        (bad-request {:error "Failed to fetch model" :details (.getMessage e)})))))
