(ns leihs.inventory.server.resources.pool.models.main
  (:require
   [clojure.set]
   [clojure.string :as string]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.middlewares.debug :refer [log-by-severity]]
   [leihs.inventory.server.middlewares.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.resources.pool.entitlement-groups.entitlement-group.query :refer [select-allocations]]
   [leihs.inventory.server.resources.pool.list.search :refer [make-multi-term-clause]]
   [leihs.inventory.server.resources.pool.models.common :refer [fetch-thumbnails-for-ids
                                                                filter-map-by-spec
                                                                model->enrich-with-image-attr]]
   [leihs.inventory.server.resources.pool.models.model.common-model-form :refer [extract-model-form-data
                                                                                 process-accessories
                                                                                 process-categories
                                                                                 process-compatibles
                                                                                 process-entitlements
                                                                                 process-properties]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [leihs.inventory.server.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.transform :refer [merge-by-id to-uuid]]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]
   [ring.util.response :refer [bad-request response]]))

(def ERROR_CREATE_MODEL "Failed to create model")
(def ERROR_GET_MODEL "Failed to get models-compatible")

(def base-query
  (-> (sql/select :models.id
                  :models.product :models.version :models.name
                  :models.cover_image_id)
      (sql/from :models)
      (sql/order-by :models.name)))

(defn get-by-id [tx id]
  (-> base-query
      (sql/where [:= :models.id id])
      sql-format
      (->> (jdbc-query tx))
      first))

(defn ensure-entitled-in-groups-default-value [coll]
  (mapv #(if (contains? % :entitled_in_groups)
           %
           (assoc % :entitled_in_groups 0))
        coll))

(defn index-resources [request]
  (try
    (let [tx (:tx request)
          pool-id (-> request path-params :pool_id)
          {:keys [search search_term type]} (query-params request)
          term (or search search_term) ; search_term needed for fields
          base-query (-> base-query
                         (sql/select [[:count :i.id] :borrowable_quantity])
                         (sql/left-join [:items :i]
                                        [:and
                                         [:= :i.model_id :models.id]
                                         [:= :i.inventory_pool_id pool-id]
                                         [:= :i.is_borrowable true]
                                         [:= :i.retired nil]
                                         [:= :i.parent_id nil]])
                         (cond-> term
                           (sql/where (make-multi-term-clause term :ilike :models.name)))
                         (cond->
                          (= type "package") (sql/where [:= :models.is_package true])
                           ; in this case either `model` or `software`
                          (and type (not= type "package")) (sql/where [:= :models.type (string/capitalize type)]))
                         (sql/group-by :models.id
                                       :models.name
                                       :models.cover_image_id))

          post-fnc (fn [models]
                     (let [model-ids (->> models
                                          (map :id)
                                          (into []))
                           allocations (if (empty? model-ids)
                                         []
                                         (select-allocations tx pool-id model-ids nil))
                           models (-> (merge-by-id models allocations)
                                      ensure-entitled-in-groups-default-value)]
                       (->> models
                            (fetch-thumbnails-for-ids tx)
                            (map (model->enrich-with-image-attr pool-id)))))]

      (response (create-pagination-response request base-query nil post-fnc)))

    (catch Exception e
      (log-by-severity ERROR_GET_MODEL e)
      (exception-handler request ERROR_GET_MODEL e))))

;###################################################################################

(defn post-resource [request]
  (let [tx (:tx request)
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        {:keys [accessories prepared-model-data categories compatibles properties entitlements]}
        (extract-model-form-data request)]

    (try
      (let [models (jdbc/execute-one! tx (-> (sql/insert-into :models)
                                             (sql/values [prepared-model-data])
                                             (sql/returning :*)
                                             sql-format))
            models (filter-map-by-spec models :create-model/scheme)
            model-id (:id models)]

        (process-entitlements tx entitlements model-id)
        (process-properties tx properties model-id)
        (process-accessories tx accessories model-id pool-id)
        (process-compatibles tx compatibles model-id)
        (process-categories tx categories model-id pool-id)

        (if models
          (response models)
          (bad-request {:message ERROR_CREATE_MODEL})))
      (catch Exception e
        (log-by-severity ERROR_CREATE_MODEL e)
        (exception-handler request ERROR_CREATE_MODEL e)))))
