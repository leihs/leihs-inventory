(ns leihs.inventory.server.resources.models.models-by-pool
  (:require
   [clojure.set]
   [honey.sql :as sq :refer [format] :rename {format sql-format}]

   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.models.queries :refer [accessories-query attachments-query base-pool-query
                                                            entitlements-query item-query
                                                            model-links-query properties-query]]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.core :refer [single-entity-get-request?]]
   [leihs.inventory.server.utils.helper :refer [convert-map-if-exist]]
   [leihs.inventory.server.utils.pagination :refer [fetch-pagination-params pagination-response create-pagination-response]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]])
  (:import [java.net URL JarURLConnection]
           (java.time LocalDateTime)
           [java.util.jar JarFile]))

(defn- extract-option-type-from-uri [input-str]
  (let [valid-segments ["properties" "items" "accessories" "attachments" "entitlements" "model-links"]
        last-segment (-> input-str
                         (clojure.string/split #"/")
                         last)]
    (if (some #(= last-segment %) valid-segments)
      last-segment
      nil)))

(defn get-models-handler
  ([request]
   (get-models-handler request false))
  ([request with-pagination?]
   (let [tx (:tx request)
         {:keys [pool_id model_id item_id properties_id accessories_id attachments_id entitlement_id model_link_id]} (path-params request)
         option-type (extract-option-type-from-uri (:uri request))
         query-params (query-params request)
         {:keys [filter_ids]} query-params
         {:keys [page size]} (fetch-pagination-params request)
         sort-by (case (:sort_by query-params)
                   :manufacturer-asc [:m.manufacturer :asc]
                   :manufacturer-desc [:m.manufacturer :desc]
                   :product-asc [:m.product :asc]
                   :product-desc [:m.product :desc]
                   nil)
         filter-manufacturer (if-not model_id (:filter_manufacturer query-params) nil)
         filter-product (if-not model_id (:filter_product query-params) nil)
         base-query (-> (sql/select-distinct :m.*)
                        ((fn [query] (base-pool-query query pool_id option-type)))
                        (cond-> (or item_id (= option-type "items"))
                          ((fn [q] (item-query q item_id))))
                        (cond-> (or properties_id (= option-type "properties"))
                          ((fn [q] (properties-query q properties_id))))
                        (cond-> (or accessories_id (= option-type "accessories"))
                          ((fn [q] (accessories-query q accessories_id option-type))))
                        (cond-> (or attachments_id (= option-type "attachments"))
                          ((fn [q] (attachments-query q attachments_id option-type))))
                        (cond-> (or entitlement_id (= option-type "entitlements"))
                          ((fn [q] (entitlements-query q entitlement_id))))
                        (cond-> (or model_link_id (= option-type "model-links"))
                          ((fn [q] (model-links-query q model_link_id pool_id))))
                        (cond-> filter-manufacturer
                          (sql/where [:ilike :m.manufacturer (str "%" filter-manufacturer "%")]))
                        (cond-> filter-product
                          (sql/where [:ilike :m.product (str "%" filter-product "%")]))
                        (cond-> model_id (sql/where [:= :m.id model_id]))
                        (cond-> filter_ids (sql/where [:in :m.id filter_ids]))
                        (cond-> (and sort-by model_id) (sql/order-by sort-by)))

         p (println ">o> abc" (-> base-query sql-format))
         ]
     (create-pagination-response request base-query with-pagination?))))


(defn replace-null-children [items]
  (mapv (fn [item]
          (if (= (:children item) [nil])
            (assoc item :children [])
            item))
    items))

;(ns your.namespace
;  (:require [next.jdbc.sql :as sql]
;   [next.jdbc :as jdbc]))

(defn get-models-handler
  ([request]
   (get-models-handler request false))
  ([request with-pagination?]
   (let [tx (:tx request)
         {:keys [pool_id model_id item_id properties_id accessories_id attachments_id entitlement_id model_link_id]} (path-params request)
         option-type (extract-option-type-from-uri (:uri request))
         query-params (query-params request)
         {:keys [filter_ids]} query-params
         {:keys [page size]} (fetch-pagination-params request)
         sort-by (case (:sort_by query-params)
                   :manufacturer-asc [:m.manufacturer :asc]
                   :manufacturer-desc [:m.manufacturer :desc]
                   :product-asc [:m.product :asc]
                   :product-desc [:m.product :desc]
                   nil)
         filter-manufacturer (if-not model_id (:filter_manufacturer query-params) nil)
         filter-product (if-not model_id (:filter_product query-params) nil)

         ;; Define base query with entry_type column
         base-query (-> (sql/select
                          :m.id
                          :m.product
                          :m.version
                          :m.is_package
                          :m.type
                          [(sq/call :array_agg :i.id) :children] ;; fixme: contains results like [null]
                          [(sq/call :case
                             [:and [:= :m.type "Model"] [:= :m.is_package false]] "Model"
                             [:and [:= :m.type "Model"] [:= :m.is_package true]] "Package"
                             [:= :m.type "Software"] "Software"
                             :else "Unknown")
                           :entry_type]

                      ;[(sq/call :case
                      ;   [(sq/call := (sq/call :array_length (sq/call :array_agg :i.id) 1) nil) true]
                      ;   :else false)
                      ; :deletable]

                          ;[(sq/call :case
                          ;   [(sq/call := (sq/call :count :i.id) 0) true]
                          ;   :else false)
                          ; :deletable]

                          ;[(sq/call :case
                          ;   [(sq/call := (sq/call :count [:raw "i.id"] :filter [:raw "WHERE i.id IS NOT NULL"]) 0) true]
                          ;   :else false)
                          ; :deletable]

                          )
                      (sql/from [:models :m])
                      (sql/left-join [:items :i] [:= :m.id :i.model_id])
                      (sql/group-by :m.id :m.product :m.version :m.is_package :m.type)
                      (cond-> filter-manufacturer
                        (sql/where [:ilike :m.manufacturer (str "%" filter-manufacturer "%")]))
                      (cond-> filter-product
                        (sql/where [:ilike :m.product (str "%" filter-product "%")]))
                      (cond-> model_id (sql/where [:= :m.id model_id]))
                      (cond-> filter_ids (sql/where [:in :m.id filter_ids]))
                      (cond-> (and sort-by model_id) (sql/order-by sort-by))

                      ;(sql/where [:= :m.is_package true])   ;; TODO: remove this


                      ;(sql/limit 20)
                      )

         p (println ">o> abc" (-> base-query sql-format))


         result (-> base-query sql-format)
         result (jdbc/execute! tx result)

         result (replace-null-children result)

         ]

     ;(create-pagination-response request base-query with-pagination?)
     result

     )))




(defn get-models-of-pool-with-pagination-handler [request]
  (response (get-models-handler request true)))

(defn get-models-of-pool-auto-pagination-handler [request]
  (response (get-models-handler request nil)))

(defn get-models-of-pool-handler [request]
  (let [result (get-models-handler request)]
    (response result)))




(defn get-items-handler
  ([request]
   (get-items-handler request false))
  ([request with-pagination?]
   (let [tx (:tx request)

         _ (println ">o> abc.get-items-handler1")


         ;{:keys [pool_id model_id item_id]} (path-params request)
         {:keys [pool_id model_id ]} (path-params request)
         _ (println ">o> abc.get-items-handler1")
         {:keys [entry_type]} (query-params request)

         ;; Debugging logs
         _ (println ">o> abc.pool_id" pool_id (type pool_id))
         _ (println ">o> abc.model_id" model_id (type model_id))
         _ (println ">o> abc.type" entry_type (type entry_type))

         ;{:keys [page size]} (fetch-pagination-params request)
         ;{:keys [search_term not_packaged packages retired result_type]} (query-params request)

         ;; HoneySQL query
         base-query (-> (sql/select :i.id
                          :i.inventory_code
                          :i.inventory_pool_id
                          :r.name
                          :i.model_id
                          :r.description
                          :b.name
                          :b.code

                          ;; this breaks the calculation
                          ;[(sq/call :coalesce
                          ;   (sq/call :array_agg
                          ;     (sq/call :filter :it.parent_id [:is-not :it.parent_id nil]))
                          ;   "{}")
                          ; :children]

                          [(sq/call :array_agg :it.parent_id) :children]

                          ;[(sq/call :case
                          ;   [:and [:= :m.type "Model"] [:= :m.is_package false]] "ModelItem"
                          ;   [:and [:= :m.type "Model"] [:= :m.is_package true]] "PackageItem"
                          ;   [:= :m.type "Software"] "SoftwareItem"
                          ;   :else "Unknown")
                          ; :entry_type]


                          ;[[[:raw (str entry_type "Item")]] :entry_type]
                          ;[:raw (str entry_type "Item") :as :entry_type]
                          ;[[:raw (str entry_type "Item") :entry_type]]

                          )
                      (sql/from [:items :i])
                      ;(sql/left-join [:model :m] [:= :m.id :i.model_id])
                      (sql/left-join [:items :it] [:= :i.id :it.parent_id])
                      (sql/left-join [:rooms :r] [:= :r.id :i.room_id])
                      (sql/left-join [:buildings :b] [:= :b.id :r.building_id])
                      ;(cond-> item_id (sql/where [:= :i.id item_id]))

                      (cond-> model_id (sql/where [:= :i.model_id model_id]))
                      ;(cond-> pool_id (sql/where [:= :i.inventory_pool_id pool_id]))

                      (sql/group-by :i.id
                        :i.inventory_code
                        :i.inventory_pool_id
                        :i.model_id
                        :r.name
                        :r.description
                        :b.name
                        :b.code

                        ;:entry_type
                        ;:m

                        )

                      )

         _ (println ">o> abc.query" (-> base-query sql-format))

         result (jdbc/execute! tx (-> base-query sql-format))

         result (replace-null-children result)

         result (mapv #(assoc % :entry_type (str entry_type "Item")) result)


         ]

     (response result))))


(defn get-item-handler
  ([request]
   (get-item-handler request false))
  ([request with-pagination?]
   (let [tx (:tx request)

         _ (println ">o> abc.get-item-handler2 !!!!")


         ;{:keys [pool_id model_id item_id]} (path-params request)
         {:keys [pool_id model_id item_id]} (path-params request)
         _ (println ">o> abc.get-items-handler1")
         {:keys [entry_type]} (query-params request)

         ;; Debugging logs
         _ (println ">o> abc.pool_id" pool_id (type pool_id))
         _ (println ">o> abc.model_id" model_id (type model_id))
         _ (println ">o> abc.type" entry_type (type entry_type))

         ;; HoneySQL query
         ;base-query (-> (sql/select :i.id
         ;                 :i.inventory_code
         ;                 :i.inventory_pool_id
         ;                 :i.model_id
         ;
         ;                 :m.product
         ;                 ;[:m.is_package :is_part_of_package]
         ;
         ;                 )
         ;             (sql/from [:items :i])
         ;             (sql/left-join [:items :it] [:= :i.id :it.parent_id])
         ;             (sql/join [:models :m] [:= :it.model_id :m.id])
         ;
         ;             (cond-> item_id (sql/where  [:= :i.parent_id item_id]))
         ;
         ;             )

         base-query (-> (sql/select :i.id
                          :i.inventory_code
                          :i.inventory_pool_id
                          :i.model_id
                          ;[:m.product :product1]
                          ;[:m2.product :product2]
                          :m2.product
                          :m2.manufacturer
                          [:m2.is_package :is_part_of_package]
                          )
                      (sql/from [:items :i])
                      (sql/left-join [:items :it] [:= :i.id :it.parent_id])  ; "it" first
                      ;(sql/left-join [:models :m] [:= :it.model_id :m.id])        ; "m" after
                      (sql/left-join [:models :m2] [:= :i.model_id :m2.id])        ; "m" after
                      (cond-> item_id (sql/where [:= :i.parent_id item_id])))


         _ (println ">o> abc.query" (-> base-query sql-format))

         result (jdbc/execute! tx (-> base-query sql-format))




         result (replace-null-children result)
         result (mapv #(assoc % :entry_type  "Item") result)

         ]

     (response result))))


;;  ------------

(defn create-model-handler-by-pool [request]
  (let [created_ts (LocalDateTime/now)
        model-id (get-in request [:path-params :model_id])
        pool-id (get-in request [:path-params :pool_id])
        body-params (:body-params request)
        tx (:tx request)
        model (assoc body-params :created_at created_ts :updated_at created_ts)
        categories (:category_ids model)
        model (dissoc model :category_ids)]
    (try
      (let [res (jdbc/execute-one! tx (-> (sql/insert-into :models)
                                          (sql/values [model])
                                          (sql/returning :*)
                                          sql-format))
            model-id (:id res)]
        (doseq [category-id categories]
          (jdbc/execute! tx (-> (sql/insert-into :model_links)
                                (sql/values [{:model_id model-id :model_group_id (to-uuid category-id)}])
                                sql-format))
          (jdbc/execute! tx (-> (sql/insert-into :inventory_pools_model_groups)
                                (sql/values [{:inventory_pool_id (to-uuid pool-id) :model_group_id (to-uuid category-id)}])
                                sql-format)))
        (if res
          (response [res])
          (bad-request {:error "Failed to create model"})))
      (catch Exception e
        (error "Failed to create model" e)
        (bad-request {:error "Failed to create model" :details (.getMessage e)})))))

(defn update-model-handler-by-pool [request]
  (let [model-id (get-in request [:path-params :model_id])
        body-params (:body-params request)
        tx (:tx request)]
    (try
      (let [res (jdbc/execute! tx (-> (sql/update :models)
                                      (sql/set (convert-map-if-exist body-params))
                                      (sql/where [:= :id (to-uuid model-id)])
                                      (sql/returning :*)
                                      sql-format))]
        (if (= 1 (count res))
          (response res)

          (bad-request {:error "Failed to update model" :details "Model not found"})))
      (catch Exception e
        (error "Failed to update model" e)
        (bad-request {:error "Failed to update model" :details (.getMessage e)})))))

(defn delete-model-handler-by-pool [request]
  (let [tx (:tx request)
        model-id (get-in request [:path-params :model_id])]
    (try
      (let [res (jdbc/execute! tx (-> (sql/delete-from :models)
                                      (sql/where [:= :id (to-uuid model-id)])
                                      (sql/returning :*)
                                      sql-format))]
        (if (= 1 (count res))
          (response res)
          (bad-request {:error "Failed to delete model" :details "Model not found"})))
      (catch Exception e
        (error "Failed to delete model" e)
        (status (bad-request {:error "Failed to delete model" :details (.getMessage e)}) 409)))))
