(ns leihs.inventory.server.resources.pool.models.list.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.core :refer [presence]]
   [leihs.inventory.server.resources.pool.models.common :refer [fetch-thumbnails-for-ids
                                                                filter-map-by-spec]]
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
   [taoensso.timbre :refer [debug]])
  (:import
   (java.time LocalDateTime)))

(defn index-resources
  ([request]
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
                     (#(from-category tx % category_id))))

         post-fnc (fn [models]
                    (->> models
                         (fetch-thumbnails-for-ids tx)
                         (map (fn [m]
                                (if-let [image-id (:image_id m)]
                                  (assoc m :url (str "/inventory/" pool_id "/models/" (:id m) "/images/" image-id)
                                         :content_type (:content_type m))
                                  m)))))]

     (debug (sql-format query :inline true))
     (response (create-pagination-response request query nil post-fnc)))))
