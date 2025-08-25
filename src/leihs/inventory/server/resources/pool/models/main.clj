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
   [taoensso.timbre :refer [debug error]]))

(defn get-resource [request]
  (try
    (let [tx (:tx request)
          pool-id (-> request path-params :pool_id)
          {:keys [search]} (query-params request)
          base-query (-> (sql/select
                          :id
                          :product
                          :version
                          :cover_image_id)
                         (sql/from :models)
                         (cond-> search
                           (sql/where [:or
                                       [:ilike :product (str "%" search "%")]
                                       [:ilike :name (str "%" search "%")]
                                       [:ilike :version (str "%" search "%")]]))
                         (sql/order-by [[:trim [:|| :product " " :version]] :asc]))

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
      (debug e)
      (error "Failed to get models-compatible" e)
      (bad-request {:error "Failed to get models-compatible" :details (.getMessage e)}))))

;###################################################################################

(defn create-model-handler [request]
  (let [tx (:tx request)
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        {:keys [accessories prepared-model-data categories compatibles attachments properties
                entitlements]}
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
        (debug e)
        (exception-to-response request e "Failed to create model")))))

(defn post-resource [request]
  (create-model-handler request))
