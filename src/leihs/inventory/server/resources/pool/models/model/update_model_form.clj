(ns leihs.inventory.server.resources.pool.models.model.update-model-form
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.common :refer [keep-attr-not-nil]]
   [leihs.inventory.server.resources.pool.models.model.common-model-form :refer :all]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response]]
   [taoensso.timbre :refer [error]]))

(def ALLOWED_RESPONSE_ATTRS
  [:description
   :is_package
   :name
   :cover_image_id
   :hand_over_note
   :internal_description
   :product
   :id
   :manufacturer
   :version
   :cover_image_id
   ;:updated_at
   ;:created_at
   :technical_detail])

(defn update-model-handler [request]
  (let [model-id (to-uuid (get-in request [:path-params :model_id]))
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        tx (:tx request)
        {:keys [prepared-model-data categories compatibles properties accessories entitlements]}
        (extract-model-form-data request)]
    (try
      (let [update-model-query (-> (sql/update :models)
                                   (sql/set prepared-model-data)
                                   (sql/where [:= :id model-id])
                                   (sql/returning :*)
                                   sql-format)
            updated-model (-> (jdbc/execute-one! tx update-model-query)
                              (filter-response [:rental_price]))
            updated-model (keep-attr-not-nil updated-model ALLOWED_RESPONSE_ATTRS)]
        (process-entitlements tx entitlements model-id)
        (process-properties tx properties model-id)
        (process-accessories tx accessories model-id pool-id)
        (process-compatibles tx compatibles model-id)
        (process-categories tx categories model-id pool-id)

        (if updated-model
          (response updated-model)
          (bad-request {:error "Failed to update model"})))
      (catch Exception e
        (error "Failed to update model" e)
        (bad-request {:error "Failed to update model" :details (.getMessage e)})))))

(defn put-resource [request]
  (update-model-handler request))
