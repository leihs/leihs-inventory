(ns leihs.inventory.server.resources.pool.list.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.core :refer [presence]]
   [leihs.inventory.server.resources.pool.list.queries :refer [base-inventory-query
                                                               filter-by-type
                                                               from-category
                                                               with-items
                                                               all-items
                                                               with-search
                                                               without-items]]

   [leihs.inventory.server.resources.pool.models.common :refer [fetch-thumbnails-for-ids
                                                                model->enrich-with-image-attr]]
   [leihs.inventory.server.utils.export :refer [csv-response]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [leihs.inventory.server.utils.request-utils :refer [path-params
                                                       query-params]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [response]]
   [taoensso.timbre :refer [debug]]))

(defn- get-accept-header [request]
  (get-in request [:headers "accept"]))

(defn index-resources
  ([request]
   (let [tx (:tx request)
         {pool-id :pool_id} (path-params request)
         {:keys [with_items type
                 retired borrowable incomplete broken
                 inventory_pool_id owned in_stock
                 category_id
                 search before_last_check]} (query-params request)
         query (-> (base-inventory-query pool-id)
                   (cond-> type (filter-by-type type))
                   (cond->
                    (not= type :option)
                     (cond->
                      (true? with_items)
                       (with-items pool-id
                         (cond-> {:retired retired
                                  :borrowable borrowable
                                  :incomplete incomplete
                                  :broken broken
                                  :inventory_pool_id inventory_pool_id
                                  :owned owned
                                  :in_stock in_stock}
                           (not= type :software)
                           (assoc :before_last_check before_last_check)))

                       (false? with_items)
                       (without-items pool-id)

                       (nil? with_items)
                       (all-items pool-id
                                  (cond-> {:retired retired
                                           :borrowable borrowable
                                           :incomplete incomplete
                                           :broken broken
                                           :inventory_pool_id inventory_pool_id
                                           :owned owned
                                           :in_stock in_stock}
                                    (not= type :software)
                                    (assoc :before_last_check before_last_check)))))
                   (cond-> (presence search)
                     (with-search search))
                   (cond-> (and category_id (not (some #{type} [:option :software])))
                     (#(from-category tx % category_id))))

         accept-header (get-accept-header request)]

     (debug (sql-format query :inline true))

     (if (and accept-header (re-find #"text/csv" accept-header))
       (let [data (-> query 
                      (cond-> with_items
                        (sql/join :items [:= :inventory.id :items.model_id]))
                      sql-format
                      (->> (jdbc/execute! tx)))]
         (csv-response data :filename "inventory-list.csv"))
       (let [post-fnc (fn [models]
                        (->> models
                             (fetch-thumbnails-for-ids tx)
                             (map (model->enrich-with-image-attr pool-id))))]
         (-> request
             (create-pagination-response query nil post-fnc)
             response))))))
