(ns leihs.inventory.server.resources.pool.list.main
  (:require
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
   [clojure.java.io :as io]
   [clojure.set]
   [clojure.set]
   [clojure.set]
   [clojure.string :as str]
   [dk.ative.docjure.spreadsheet :as ss]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.core :refer [presence]]
   [leihs.inventory.server.resources.pool.fields.main :refer [fetch-properties-fields]]
   [leihs.inventory.server.resources.pool.items.filter-handler :as filter]
   [leihs.inventory.server.resources.pool.items.main :as helper]
   [leihs.inventory.server.resources.pool.list.export-csv :as export-csv]
   [leihs.inventory.server.resources.pool.list.export-excel :as export-excel]
   [leihs.inventory.server.resources.pool.models.common :refer [fetch-thumbnails-for-ids
                                                                model->enrich-with-image-attr]]
   [leihs.inventory.server.resources.pool.models.queries :refer [base-inventory-query
                                                                 filter-by-type
                                                                 from-category
                                                                 with-items
                                                                 with-search
                                                                 without-items]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [leihs.inventory.server.utils.request-utils :refer [path-params
                                                       query-params]]
   [ring.middleware.accept]
   [ring.util.response :refer [response]]
   [taoensso.timbre :refer [debug]]))

(defn items-sub-query [query]
  (-> query
    (sql/select :items.properties)
    (sql/right-join :items [:= :items.model_id :inventory.id])))

(defn index-resources
  ([request]
   (let [tx (:tx request)
         {pool-id :pool_id} (path-params request)
         {:keys [with_items type
                 retired borrowable incomplete broken
                 inventory_pool_id owned in_stock
                 category_id
                 search before_last_check filters]} (query-params request)

         accept-type (-> request :accept :mime)
         p (println ">o> abc.accept-type2" accept-type)

         p (println ">o> abc.before, filters" filters)
         parsed-filters (filter/parse-json-param filters)
         p (println ">o> abc.after, filters" parsed-filters)

         properties-fields (fetch-properties-fields request)
         p (println ">o> abc.properties-fields.count" (first properties-fields))

         {:keys [filter-keys properties raw-filter-keys]} (helper/extract-ids properties-fields "properties_")
         WHITELIST-ITEM-FILTER filter-keys
         p (println ">o> abc.WHITELIST-ITEM-FILTER2" WHITELIST-ITEM-FILTER)

         parsed-filters (helper/prepare-filters parsed-filters)
         p (println ">o> abc.parsed-filters" parsed-filters)
         validation-result (filter/validate-filters parsed-filters WHITELIST-ITEM-FILTER)

         query (-> (base-inventory-query pool-id)
                 (cond-> type (filter-by-type type))
                 (cond->
                   (not= type :option)
                   (cond->
                     (and pool-id (true? with_items))
                     (with-items pool-id
                       (cond-> {:retired retired
                                :borrowable borrowable
                                :incomplete incomplete
                                :broken broken
                                :inventory_pool_id inventory_pool_id
                                :owned owned
                                :in_stock in_stock
                                }
                         (not= type :software)
                         (assoc :before_last_check before_last_check)))
                     (and pool-id (false? with_items))
                     (without-items pool-id)))

                 (cond-> (seq parsed-filters)
                   (-> items-sub-query
                     (filter/add-filter-groups parsed-filters raw-filter-keys)))

                 (cond-> (and pool-id (presence search))
                   (with-search search))
                 (cond-> (and category_id (not (some #{type} [:option :software])))
                   (#(from-category tx % category_id))))

         post-fnc (fn [models]
                    (->> models
                      (fetch-thumbnails-for-ids tx)
                      (map (model->enrich-with-image-attr pool-id))))]
     (debug (sql-format query :inline true))

     (cond
       (= accept-type :csv) (export-csv/convert (create-pagination-response request query false post-fnc))
       (= accept-type :excel) (export-excel/convert (create-pagination-response request query false post-fnc))
       :else (response (create-pagination-response request query nil post-fnc))))))
