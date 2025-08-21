(ns leihs.inventory.server.resources.pool.models.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.models.common :refer [fetch-thumbnails-for-ids
                                                                filter-map-by-spec]]
   [leihs.inventory.server.resources.pool.models.model.common-model-form :refer [extract-model-form-data
                                                                                 process-accessories
                                                                                 process-categories
                                                                                 process-compatibles
                                                                                 process-entitlements
                                                                                 process-properties]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.exception-handler :refer [exception-to-response]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [leihs.inventory.server.utils.request-utils :refer [path-params
                                                       query-params]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response]]
   [taoensso.timbre :refer [error]])
  (:import
   (java.time LocalDateTime)))

(defn index-resources [request]
  (try
    (let [tx (:tx request)
          pool-id (-> request path-params :pool_id)
          {:keys [search]} (query-params request)
          base-query (-> (sql/select
                          :models.id
                          :models.product
                          :models.version
                          :models.cover_image_id
                          [[:count :items.id] :available])
                         (sql/from :models)
                         (sql/left-join :items
                                        [:and
                                         [:= :items.model_id :models.id]
                                         [:= :items.inventory_pool_id pool-id]
                                         [:= :items.is_borrowable true]
                                         [:= :items.retired nil]
                                         [:= :items.parent_id nil]])
                         (cond-> search
                           (sql/where [:or
                                       [:ilike :models.product (str "%" search "%")]
                                       [:ilike :models.name (str "%" search "%")]
                                       [:ilike :models.version (str "%" search "%")]]))
                         (sql/group-by :models.id
                                       :models.product
                                       :models.version
                                       :models.cover_image_id)
                         (sql/order-by [[:trim [:|| :models.product " " :models.version]] :asc]))

          post-fnc (fn [models]
                     (->> models
                          (fetch-thumbnails-for-ids tx)
                          (map (fn [m]
                                 (if-let [image-id (:image_id m)]
                                   (assoc m :url (str "/inventory/" pool-id "/models/" (:id m) "/images/" image-id)
                                          :content_type (:content_type m))
                                   m)))))]

      (response (create-pagination-response request base-query nil post-fnc)))

    (catch Exception e
      (error "Failed to get models-compatible" e)
      (bad-request {:error "Failed to get models-compatible" :details (.getMessage e)}))))

;###################################################################################

(defn post-resource [request]
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
