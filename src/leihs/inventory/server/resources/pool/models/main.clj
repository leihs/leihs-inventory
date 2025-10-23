(ns leihs.inventory.server.resources.pool.models.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.models.common :refer [fetch-thumbnails-for-ids
                                                                filter-map-by-spec
                                                                model->enrich-with-image-attr]]
   [leihs.inventory.server.resources.pool.models.model.common-model-form :refer [extract-model-form-data
                                                                                 process-accessories
                                                                                 process-categories
                                                                                 process-compatibles
                                                                                 process-entitlements
                                                                                 process-properties]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.helper :refer [log-by-severity]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [leihs.inventory.server.utils.request-utils :refer [path-params
                                                       query-params]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response]]))

(def ERROR_CREATE_MODEL "Failed to create model")
(def ERROR_GET_MODEL "Failed to get models-compatible")

(def base-query
  (-> (sql/select :id :product :version :name :cover_image_id)
      (sql/from :models)
      (sql/order-by :name)))

(defn index-resources [request]
  (try
    (let [tx (:tx request)
          pool-id (-> request path-params :pool_id)
          {:keys [search search_term]} (query-params request)
          term (or search search_term) ; search_term needed for fields
          base-query (-> base-query
                         (sql/select [[:count :i.id] :available])
                         (sql/left-join [:items :i]
                                        [:and
                                         [:= :i.model_id :models.id]
                                         [:= :i.inventory_pool_id pool-id]
                                         [:= :i.is_borrowable true]
                                         [:= :i.retired nil]
                                         [:= :i.parent_id nil]])
                         (cond-> search
                           (sql/where [:ilike :models.name (str "%" term "%")]))
                         (sql/group-by :models.id
                                       :models.product
                                       :models.version
                                       :models.cover_image_id))

          post-fnc (fn [models]
                     (->> models
                          (fetch-thumbnails-for-ids tx)
                          (map (model->enrich-with-image-attr pool-id))))]

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
          (bad-request {:message ERROR_CREATE_MODEL})))
      (catch Exception e
        (log-by-severity ERROR_CREATE_MODEL e)
        (exception-handler request ERROR_CREATE_MODEL e)))))
