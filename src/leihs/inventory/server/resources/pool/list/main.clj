(ns leihs.inventory.server.resources.pool.list.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [leihs.core.core :refer [presence]]
   [leihs.inventory.server.constants :refer [ACCEPT-CSV ACCEPT-EXCEL]]
   [leihs.inventory.server.resources.pool.list.export :as list-export]
   [leihs.inventory.server.resources.pool.list.queries :refer [base-inventory-query
                                                               filter-by-type
                                                               from-category
                                                               select-items-count
                                                               with-items
                                                               without-items]]
   [leihs.inventory.server.resources.pool.list.search :refer [with-search-inventory]]

   [leihs.inventory.server.resources.pool.models.common :refer [fetch-thumbnails-for-ids
                                                                model->enrich-with-image-attr]]
   [leihs.inventory.server.utils.export :as export]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [leihs.inventory.server.utils.request-utils :refer [path-params
                                                       query-params]]
   [ring.util.response :refer [response]]
   [taoensso.timbre :refer [debug]]))

(defn- get-accept-header [request]
  (get-in request [:headers "accept"]))

(def EXPORT-FILE-NAME "inventory-list")

(defn index-resources [request]
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
                                 :in_stock in_stock
                                 :search search}
                          (not= type :software)
                          (assoc :before_last_check before_last_check)))

                      (false? with_items)
                      (without-items pool-id)

                      (nil? with_items)
                      (select-items-count pool-id
                                          (cond-> {:retired retired
                                                   :borrowable borrowable
                                                   :incomplete incomplete
                                                   :broken broken
                                                   :inventory_pool_id inventory_pool_id
                                                   :owned owned
                                                   :in_stock in_stock
                                                   :search search}
                                            (not= type :software)
                                            (assoc :before_last_check before_last_check)))))
                  (cond-> (presence search)
                    (with-search-inventory search))
                  (cond-> (and category_id (not (some #{type} [:option :software])))
                    (#(from-category tx % category_id))))

        accept-header (get-accept-header request)]

    (debug (sql-format query :inline true))

    (cond
      (and accept-header (re-find (re-pattern ACCEPT-CSV) accept-header))
      (let [data (-> query
                     (#(list-export/sql-prepare tx % pool-id))
                     sql-format
                     (->> (export/jdbc-execute! tx)))]
        (export/csv-response data :filename (str EXPORT-FILE-NAME ".csv")))

      (and accept-header (re-find (re-pattern ACCEPT-EXCEL) accept-header))
      (let [array-data (-> query
                           (#(list-export/sql-prepare tx % pool-id))
                           sql-format
                           (->> (export/jdbc-execute! tx)))
            [header & _] array-data
            data (export/arrays-to-maps array-data)]
        (export/excel-response data
                               :keys (map keyword header)
                               :filename (str EXPORT-FILE-NAME ".xlsx")))

      :else
      (let [post-fnc (fn [models]
                       (->> models
                            (fetch-thumbnails-for-ids tx)
                            (map (model->enrich-with-image-attr pool-id))))]
        (-> request
            (create-pagination-response query nil post-fnc)
            response)))))
