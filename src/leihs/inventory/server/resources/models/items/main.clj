(ns leihs.inventory.server.resources.models.items.main
  (:require
   [clojure.set]
   [clojure.string :as str]
   [honey.sql :refer [format]
    :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.models.form.model.common :refer [create-image-url]]
   [leihs.inventory.server.resources.models.helper :refer [str-to-bool]]
   [leihs.inventory.server.resources.models.models-by-pool :refer [apply-is_deleted-context-if-valid
                                                                   apply-is_deleted-where-context-if-valid]]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.helper :refer [convert-map-if-exist]]
   [leihs.inventory.server.utils.pagination :refer [create-paginated-response create-pagination-response
                                                    fetch-pagination-params fetch-pagination-params-raw]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [spy debug error]]))

(defn base-query [pool-id model-id item-id]
  (-> (sql/select :*)
      (sql/from :items)
      (sql/where [:= :items.model_id model-id])
      (cond-> item-id
        (sql/where [:= :items.id item-id]))
      (cond-> pool-id
        (sql/where [:or
                    [:= :items.inventory_pool_id pool-id]
                    [:= :items.owner_id pool-id]]))))

(defn get-model-items-handler
  ([request]
   (get-model-items-handler request false))
  ([request with-pagination?]
   (debug "get-model-items-handler")
   (let [tx (:tx request)
         {:keys [pool_id model_id item_id]} (path-params request)
         {:keys [page size]} (fetch-pagination-params request)
         query (base-query pool_id model_id item_id)]
     (create-pagination-response request query with-pagination?))))

(defn get-model-items-with-pagination-handler [request]
  (response (get-model-items-handler request true)))

(defn get-model-items-auto-pagination-handler [request]
  (response (get-model-items-handler request nil)))
