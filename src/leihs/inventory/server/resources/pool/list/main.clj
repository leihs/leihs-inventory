(ns leihs.inventory.server.resources.pool.list.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [leihs.core.core :refer [presence]]
   [leihs.inventory.server.resources.pool.models.common :refer [fetch-thumbnails-for-ids]]
   [leihs.inventory.server.resources.pool.models.queries :refer [base-inventory-query
                                                                 filter-by-type
                                                                 from-category
                                                                 with-items
                                                                 with-search
                                                                 without-items]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response
                                                    fetch-pagination-params]]
   [leihs.inventory.server.utils.request-utils :refer [path-params
                                                       query-params]]
   [ring.util.response :refer [response]]
   [taoensso.timbre :refer [debug]]))

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
                    (not= type :option)
                     (cond->
                      (and pool_id (true? with_items))
                       (with-items pool_id
                         (cond-> {:retired retired
                                  :borrowable borrowable
                                  :incomplete incomplete
                                  :broken broken
                                  :inventory_pool_id inventory_pool_id
                                  :owned owned
                                  :in_stock in_stock}
                           (not= type :software)
                           (assoc :before_last_check before_last_check)))
                       (and pool_id (false? with_items))
                       (without-items pool_id)))
                   (cond-> (and pool_id (presence search))
                     (with-search search))
                   (cond-> (and category_id (not (some #{type} [:option :software])))
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
