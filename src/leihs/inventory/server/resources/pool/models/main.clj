(ns leihs.inventory.server.resources.pool.models.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.core :refer [presence]]
   [leihs.inventory.server.resources.pool.models.common :refer [fetch-thumbnails-for-ids
                                                                filter-map-by-spec
                                                                ]]

   ;[leihs.inventory.server.utils.pagination :refer [create-paginated-response
   ;                                                 fetch-pagination-params]]
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
                                                    fetch-pagination-params-raw
                                                    ]]
   [leihs.inventory.server.utils.request-utils :refer [path-params
                                                       query-params]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [debug error]])
  (:import
   (java.time LocalDateTime)))

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

     (if (url-ends-with-uuid? (:uri request))
       (let [res (jdbc/execute-one! tx (-> query sql-format))]
         (if res
           (response res)
           (status 404)))
       (response (create-pagination-response request query with-pagination? post-fnc))))))


(defn get-resource [request]
  (try
    (let [tx (:tx request)
          ;model_id (-> request path-params :model_id)
          pool-id (-> request path-params :pool_id)
          {:keys [search]} (query-params request)
          base-query (-> (sql/select
                           ;[:id :model_id]
                           :id
                           :product
                           :version
                           ;(create-image-url :models :cover_image_url)
                           :cover_image_id)
                       (sql/from :models)
                       ;(sql/left-join :images [:= :cover_image_id :images.id])
                       (cond-> search
                         (sql/where [:or

                                     [:ilike :product (str "%" search "%")]
                                     [:ilike :name (str "%" search "%")]
                                     [:ilike :version (str "%" search "%")]

                                     ]))


                       (sql/limit 10)

                       ;(cond-> model_id
                       ;  (-> (sql/join :models_compatibles [:= :models_compatibles.model_id model_id])
                       ;    (sql/where [:= :id model_id])))
                       (sql/order-by [[:trim [:|| :product " " :version]] :asc]))
          ;]

          post-fnc (fn [models]
                     (->> models
                       (fetch-thumbnails-for-ids tx)
                       (map (fn [m]
                               (println ">o> abc.m" m)
                              (if-let [image-id (:image_id m)]
                                (assoc m :url (str "/inventory/" pool-id "/models/" (:id m) "/images/" image-id)
                                  :content_type (:content_type m))
                                m)))))


          {:keys [page size]} (fetch-pagination-params-raw request)
          with-pagination? (not (and (nil? page) (nil? size)))

          ]

      ;      ;(if (or model_id (and (nil? page) (nil? size)))
      ;      (if (and (nil? page) (nil? size))
      ;        (-> (jdbc/execute! tx (-> base-query sql-format))
      ;              post-fnc
      ;          ;remove-nil-entries-fnc
      ;          response)
      ;        ;(response (create-paginated-response base-query tx size page remove-nil-entries-fnc))))
      ;        ;(response (create-paginated-response base-query tx size page post-fnc ))
      ;
      ;        (response (create-pagination-response request base-query with-pagination? post-fnc))
      ;
      ;)


      (response (create-pagination-response request base-query with-pagination? post-fnc))

      )
    (catch Exception e
      (error "Failed to get models-compatible" e)
      (bad-request {:error "Failed to get models-compatible" :details (.getMessage e)}))))



;(defn index-resources [request]
;  (get-models-handler request true))

;###################################################################################

(defn create-model-handler [request]
  (let [created-ts (LocalDateTime/now)
        tx (:tx request)
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        {:keys [accessories prepared-model-data categories compatibles attachments properties
                entitlements images new-images-attr existing-images-attr]}
        (extract-model-form-data request)]

    (try
      (let [res (jdbc/execute-one! tx (-> (sql/insert-into :models)
                                        (sql/values [prepared-model-data])
                                        (sql/returning :*)
                                        sql-format))
            res (filter-map-by-spec res :create-model/scheme)
            model-id (:id res)]

        (process-entitlements tx entitlements model-id)
        (process-properties tx properties model-id)
        (process-accessories tx accessories model-id pool-id)
        (process-compatibles tx compatibles model-id)
        (process-categories tx categories model-id pool-id)
        (if res
          (response res)
          (bad-request {:error "Failed to create model"})))
      (catch Exception e

        (exception-to-response request e "Failed to create model")))))
(defn post-resource [request]
  (create-model-handler request))
