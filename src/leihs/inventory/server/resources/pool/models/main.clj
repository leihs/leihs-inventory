(ns leihs.inventory.server.resources.pool.models.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.core :refer [presence]]
   [leihs.inventory.server.resources.pool.common :refer [keep-attr-not-nil]]
   [leihs.inventory.server.resources.pool.models.common :refer [apply-cover-image-urls create-url fetch-thumbnails-for-ids
                                                                remove-nil-values]]
   [leihs.inventory.server.resources.pool.models.model.common-model-form :refer [extract-model-form-data
                                                                                 create-validation-response
                                                                                 process-entitlements
                                                                                 process-properties
                                                                                 process-accessories
                                                                                 process-compatibles
                                                                                 process-categories]]
   [leihs.inventory.server.resources.pool.models.queries :refer [base-inventory-query
                                                                 filter-by-type
                                                                 from-category with-items with-search
                                                                 without-items]]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.exception-handler :refer [exception-to-response]]
   [leihs.inventory.server.utils.helper :refer [url-ends-with-uuid?]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response fetch-pagination-params]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [debug error]])
  (:import [java.net URL JarURLConnection]
           (java.time LocalDateTime)
           [java.util UUID]
           [java.util.jar JarFile]))

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
                    (println ">o> abc.models1" (first models))
                    (println ">o> abc.models2" (map #(select-keys % [:id :cover_image_id]) models))

                    (->> models
                         (fetch-thumbnails-for-ids tx)
                         (map (fn [m]
                                (if-let [image-id (:image_id m)]
                                  (assoc m :url (str "/inventory/" pool_id "/models/" (:id m) "/images/" image-id))
                                  m)))
                         remove-nil-values))]

     (debug (sql-format query :inline true))

     (if (url-ends-with-uuid? (:uri request))
       (let [res (jdbc/execute-one! tx (-> query sql-format))]
         (if res
           (response res)
           (status 404)))
       (response (create-pagination-response request query with-pagination? post-fnc))))))

(defn index-resources [request]
  (get-models-handler request true))

;###################################################################################

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
   ;:updated_at
   ;:created_at
   :technical_detail])

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
            res (keep-attr-not-nil res ALLOWED_RESPONSE_ATTRS)

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
