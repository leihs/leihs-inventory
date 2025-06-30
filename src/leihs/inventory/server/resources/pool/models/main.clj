(ns leihs.inventory.server.resources.pool.models.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.core :refer [presence]]
   [leihs.inventory.server.resources.pool.models.queries :refer [accessories-query attachments-query base-inventory-query
                                                            entitlements-query item-query
                                                            model-links-query properties-query
                                                            with-items without-items with-search filter-by-type
                                                            from-category]]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.core :refer [single-entity-get-request?]]
   [leihs.inventory.server.utils.helper :refer [convert-map-if-exist url-ends-with-uuid?]]
   [leihs.inventory.server.utils.pagination :refer [fetch-pagination-params pagination-response create-pagination-response]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [debug error]])
  (:import [java.net URL JarURLConnection]
           (java.time LocalDateTime)
           [java.util.jar JarFile]))


;(defn apply-is_deleted-context-if-valid
;  "setups base-query for is_deletable and references:
;  - m: models
;  - i: items
;  - it: items (for items that are children of item i)"
;  [is_deletable]
;  (-> (sql/select-distinct :m.*
;                           [[:raw "CASE
;                                   WHEN m.is_package = true AND m.type = 'Model' AND i.id IS NULL AND it.id IS NULL THEN true
;                                   WHEN m.is_package = false AND m.type = 'Model' AND i.id IS NULL AND it.id IS NULL THEN true
;                                   WHEN m.is_package = false AND m.type = 'Software' AND i.id IS NULL AND it.id IS NULL THEN true
;                                   ELSE false
;                                   END"]
;                            :is_deletable])
;      (sql/from [:models :m])
;      (sql/left-join [:items :i] [:= :m.id :i.model_id])
;      (sql/left-join [:items :it] [:= :it.parent_id :i.id])))
;
;(defn apply-is_deleted-where-context-if-valid [base-query is_deletable]
;  (if (nil? is_deletable)
;    base-query
;    (-> (sql/select-distinct :*)
;        (sql/from [[base-query] :wrapped_query])
;        (sql/where [:= :wrapped_query.is_deletable is_deletable]))))

;; THIS by pool
(defn get-models-handler
  ([request]
   (get-models-handler request false))
  ([request with-pagination?]
   (let [tx (:tx request)
         {:keys [pool_id]} (path-params request)
         {:keys [with_items type
                 retired borrowable incomplete broken
                 inventory_pool_id owned in_stock
                 category_id
                 search before_last_check]} (query-params request)
         {:keys [page size]} (fetch-pagination-params request)
         query (-> (base-inventory-query pool_id)
                   (cond-> type (filter-by-type type))
                   (cond->
                    (and pool_id (true? with_items))
                     (with-items pool_id
                       :retired retired
                       :borrowable borrowable
                       :incomplete incomplete
                       :broken broken
                       :inventory_pool_id inventory_pool_id
                       :owned owned
                       :in_stock in_stock
                       :before_last_check before_last_check)

                     (and pool_id (false? with_items))
                     (without-items pool_id)

                     (and pool_id (presence search))
                     (with-search search))
                   (cond-> category_id
                     (#(from-category tx % category_id))))]
     (debug (sql-format query :inline true))

     (if (url-ends-with-uuid? (:uri request))
       (let [res (jdbc/execute-one! tx (-> query sql-format))]
         (if res
           (response res)
           (status 404)))
       (response (create-pagination-response request query with-pagination?))))))

(defn get-models-of-pool-with-pagination-handler [request]
  (get-models-handler request true))
