(ns leihs.inventory.server.resources.pool.list.main
  (:require
   ;[clojure.data.csv :as csv]
   ;[clojure.java.io :as io]
   [clojure.set]
   ;[clojure.string :as str]
   ;[dk.ative.docjure.spreadsheet :as ss]
   ;[honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.core :refer [presence]]
   [leihs.inventory.server.resources.pool.fields.main :refer [fetch-properties-fields]]
   [leihs.inventory.server.resources.pool.list.export-csv :as export-csv]
   [leihs.inventory.server.resources.pool.list.export-excel :as export-excel]
   [leihs.inventory.server.resources.pool.list.filter-handler :as filter :refer [parse-json-param
                                                                                 validate-filters
                                                                                 add-filter-groups]]
   [leihs.inventory.server.resources.pool.list.helper :as helper :refer [extract-ids prepare-filters]]
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
   [next.jdbc :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [response]]))

(defn items-sub-query [query]
  (-> query
      (sql/select :items.properties [:items.inventory_code :iinventory_code]
                  [:items.id :item_id]
                  [:items.is_broken :is_broken]
                  [:suppliers.name :supplier_name]
        ;; TODO: owner-name, model-name, building-name, room-name not yet implemented
                  )
      (sql/right-join :items [:= :items.model_id :inventory.id])
      (sql/right-join :suppliers [:= :items.supplier_id :suppliers.id])))

(defn test [a]

  (println ">o> abc.a??" (type a) a))

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
         parsed-filters (parse-json-param filters)
         ;properties-fields (fetch-properties-fields request)

;result (->
         ;  (sql/select :f.id :f.active :f.dynamic)
         ;  (sql/from [:fields :f])
         ;  (sql/left-join [:disabled_fields :df] [:= :df.field_id :f.id])
         ;   (sql/where [:= :f.active true])
         ;   (sql/where [:not-in :f.id [select id from disabled_fields where inventory_pool_id={pool-id}]])
         ;   ;(sql/where [:and [:= :df.inventory_pool_id pool-id] [:= :f.active true]])
         ;  sql-format
         ;  ;(->> (jdbc/query tx)
         ;  (->> (jdbc/execute! tx)
         ;  ))

         _ (test pool-id)
         ;p (println ">o> abc.pool-id" )

         ;result (->
         ;         (sql/select :f.id :f.active :f.dynamic)
         ;         (sql/from [:fields :f])
         ;         (sql/left-join [:disabled_fields :df] [:= :df.field_id :f.id])
         ;         (sql/where [:= :f.active true])
         ;         (sql/where [:not-in :f.id
         ;                     (-> (sql/select :inventory_pool_id)
         ;                       (sql/from :disabled_fields)
         ;                       (sql/where [:= :inventory_pool_id pool-id ]))])
         ;         sql-format
         ;         (->> (jdbc/execute! tx)))

         result (-> (sql/select :f.id :f.active :f.dynamic)
                    (sql/from [:fields :f])
                    (sql/left-join [:disabled_fields :df]
                                   [:and
                                    [:= :f.id :df.field_id]
                ;[:= :df.inventory_pool_id "8bd16d45-056d-5590-bc7f-12849f034351"]])
                                    [:= :df.inventory_pool_id pool-id]])
                    (sql/where [:= :f.active true])
                    (sql/where [:= :df.field_id nil])
                    sql-format
                    (->> (jdbc/execute! tx)))

         p (println ">o> abc.result" result)

                  ;filter-keys     (mapv :id result)
         filter-keys (mapv (comp keyword str :id) result)

         p (println ">o> abc.filter-keys1" filter-keys)

;{:keys [filter-keys raw-filter-keys]} (extract-ids properties-fields "properties_")
         WHITELIST-ITEM-FILTER filter-keys

         p (println ">o> abc.filter-keys2" parsed-filters)
         parsed-filters (prepare-filters parsed-filters)

         p (println ">o> abc.?1")
         ;validation-result (validate-filters parsed-filters WHITELIST-ITEM-FILTER)
         ;
         ;_ (println ">o> abc.validation-result" validation-result)
         ;_ (when-not (empty? (:invalid validation-result))
         ;    (throw (ex-info "Invalid filter keys"
         ;                    {:type :invalid-filters
         ;                     :invalid-keys (:invalid-keys validation-result)})))

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
                                  :in_stock in_stock}
                           (not= type :software)
                           (assoc :before_last_check before_last_check)))
                       (and pool-id (false? with_items))
                       (without-items pool-id)))

                   (cond-> (seq parsed-filters)
                     (-> items-sub-query
                         (add-filter-groups parsed-filters filter-keys)))
                         ;(add-filter-groups parsed-filters raw-filter-keys)))

                   (cond-> (and pool-id (presence search))
                     (with-search search))
                   (cond-> (and category_id (not (some #{type} [:option :software])))
                     (#(from-category tx % category_id))))

         post-fnc (fn [models]
                    (->> models
                         (fetch-thumbnails-for-ids tx)
                         (map (model->enrich-with-image-attr pool-id))))]

     (cond
       (= accept-type :csv)
       (export-csv/convert (create-pagination-response request query false post-fnc))

       (= accept-type :excel)
       (export-excel/convert (create-pagination-response request query false post-fnc))

       :else
       (response (create-pagination-response request query nil post-fnc))))))
