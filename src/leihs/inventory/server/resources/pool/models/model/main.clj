(ns leihs.inventory.server.resources.pool.models.model.main
  (:require
   [clojure.set]
   [clojure.string :as str]
   [honey.sql :refer [format]
    :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   ;[leihs.inventory.server.resources.pool.models.basic_coercion :as co]

   [leihs.inventory.server.resources.pool.common :refer [str-to-bool remove-nil-entries-fnc remove-nil-entries
                                                         apply-is_deleted-context-if-valid
                                                         apply-is_deleted-where-context-if-valid
                                                         keep-attr-not-nil]]

;[leihs.inventory.server.resources.pool.common :refer [remove-nil-entries-fnc]]
   [leihs.inventory.server.resources.pool.models.basic_coercion :as co]
   ;[leihs.inventory.server.resources.pool.models.common :refer [  filter-map-by-spec]]

   [leihs.inventory.server.resources.pool.models.common :refer [apply-cover-image-urls fetch-thumbnails-for-ids
                                                                remove-nil-values
                                                                filter-map-by-spec

                                                                filter-and-coerce-by-spec]]
   ;[leihs.inventory.server.resources.pool.common :refer [keep-attr-not-nil]]
   [leihs.inventory.server.resources.pool.models.model.common-model-form :refer [prepare-model-data
                                                                                 extract-model-form-data
                                                                                 filter-response
                                                                                 process-entitlements
                                                                                 process-properties
                                                                                 process-accessories
                                                                                 process-compatibles
                                                                                 process-categories

                                                                                 replace-nil-with-empty-string]]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]

   [leihs.inventory.server.utils.helper :refer [convert-map-if-exist url-ends-with-uuid?]]
   [leihs.inventory.server.utils.pagination :refer [create-paginated-response fetch-pagination-params fetch-pagination-params-raw]]

   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [debug error spy]])
  (:import [java.net URL JarURLConnection]
           (java.time LocalDateTime)
           [java.util.jar JarFile]))

(defn select-entries [tx table columns where-clause]
  (jdbc/execute! tx
                 (-> (apply sql/select columns)
                     (sql/from table)
                     (sql/where where-clause)
                     sql-format)))

(defn fetch-attachments [tx model-id pool-id]

  (let [attachments (->> (select-entries tx :attachments [:id :filename] [:= :model_id model-id])
                         (map #(assoc % :url (str "/inventory/" pool-id "/models/" model-id "/attachments/" (:id %)))))]
    (filter-and-coerce-by-spec attachments ::co/attachment)))

(defn fetch-image-attributes [tx model-id pool-id]
  (let [query (-> (sql/select
                   :i.id
                   :i.filename
                   [[[:raw "CASE WHEN m.cover_image_id = i.id THEN TRUE ELSE FALSE END"]]
                    :is_cover])
                  (sql/from [:models :m])
                  (sql/right-join [:images :i] [:= :i.target_id :m.id])
                  (sql/where [:and [:= :m.id model-id] [:= :i.thumbnail false]])
                  sql-format)
        images (jdbc/execute! tx query)
        images-with-urls (mapv (fn [{:keys [id] :as row}]
                                 (assoc row
                                        :url (str "/inventory/" pool-id "/models/" model-id "/images/" id)))
                               images)
        filtered-images (filter-and-coerce-by-spec images-with-urls ::co/image)]
    filtered-images))

(defn fetch-accessories [tx model-id]
  (let [query (-> (sql/select :a.id :a.name)
                  (sql/from [:accessories :a])
                  (sql/left-join [:accessories_inventory_pools :aip] [:= :a.id :aip.accessory_id])
                  (sql/where [:= :a.model_id model-id])
                  (sql/order-by :a.name)
                  sql-format)
        accessories (jdbc/execute! tx query)]
    (mapv #(filter-map-by-spec % ::co/accessory) accessories)))

(defn fetch-compatibles [tx model-id pool-id]
  (let [query (-> (sql/select :mm.id :mm.product :mm.version ["models" :origin_table] :mm.cover_image_id)
                  (sql/from [:models_compatibles :mc])
                  (sql/left-join [:models :mm] [:= :mc.compatible_id :mm.id])
                  (sql/where [:= :mc.model_id model-id])
                  sql-format)
        models (jdbc/execute! tx query)
        models (->> models
                    (fetch-thumbnails-for-ids tx)
                    (map (fn [m]
                           (if-let [image-id (:image_id m)]
                             (assoc m :url (str "/inventory/" pool-id "/models/" (:id m) "/images/" image-id))
                             m))))
        ;models (remove-nil-values models)
        models (mapv #(filter-map-by-spec % ::co/compatible) models)]
    models))

(defn fetch-properties [tx model-id]
  (let [properties (select-entries tx :properties [:id :key :value] [:= :model_id model-id])]
    (filter-and-coerce-by-spec properties ::co/property)))

(defn fetch-entitlements [tx model-id]
  (let [query (-> (sql/select :e.id :e.quantity :eg.name [:eg.id :group_id])
                  (sql/from [:entitlements :e])
                  (sql/join [:entitlement_groups :eg] [:= :e.entitlement_group_id :eg.id])
                  (sql/where [:= :e.model_id model-id])
                  sql-format)

        entitlements (jdbc/execute! tx query)]

    (filter-and-coerce-by-spec entitlements :json/entitlement)))

(defn fetch-categories [tx model-id]
  (let [category-type "Category"
        query (-> (sql/select :mg.id :mg.type :mg.name)
                  (sql/from [:model_groups :mg])
                  (sql/left-join [:model_links :ml] [:= :mg.id :ml.model_group_id])
                  (sql/where [:ilike :mg.type (str category-type)])
                  (sql/where [:= :ml.model_id model-id])
                  (sql/order-by :mg.name)
                  sql-format)
        categories (jdbc/execute! tx query)]

    (filter-and-coerce-by-spec categories ::co/category)))

(defn get-resource [request]
  (let [current-timestamp (LocalDateTime/now)
        tx (get-in request [:tx])
        model-id (to-uuid (get-in request [:path-params :model_id]))
        pool-id (to-uuid (get-in request [:path-params :pool_id]))]
    (try
      (let [model-query (-> (sql/select :m.id :m.product :m.manufacturer :m.version :m.type
                                        :m.hand_over_note :m.description :m.internal_description
                                        :m.technical_detail :m.is_package)
                            (sql/from [:models :m])
                            (sql/where [:= :m.id model-id])
                            sql-format)
            model-result (jdbc/execute-one! tx model-query)

            attachments (fetch-attachments tx model-id pool-id)
            image-attributes (fetch-image-attributes tx model-id pool-id)
            accessories (fetch-accessories tx model-id)
            compatibles (fetch-compatibles tx model-id pool-id)
            properties (fetch-properties tx model-id)
            entitlements (fetch-entitlements tx model-id)
            categories (fetch-categories tx model-id)
            result (if model-result
                     (-> (assoc model-result
                                :attachments attachments
                                :accessories accessories
                                :compatibles compatibles
                                :properties properties
                                :images image-attributes
                                :entitlements entitlements
                                :categories categories)
                         remove-nil-values)
                     nil)]
        (if result
          (response result)
          (status
           (response {:status "failure" :message "No entry found"}) 404)))
      (catch Exception e
        (error "Failed to fetch model" e)
        (bad-request {:error "Failed to fetch model" :details (.getMessage e)})))))

; ##################################

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
            updated-model (filter-map-by-spec updated-model :create-model/scheme)]
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

; ##################################

(defn db-operation
  "Executes a SELECT or DELETE operation on the given table based on the operation keyword using next.jdbc and HoneySQL."
  [tx operation table where-clause]
  (let [query (case operation
                :select
                (-> (sql/select :*)
                    (sql/from (keyword table))
                    (sql/where where-clause)
                    sql-format)
                :delete (-> (sql/delete-from table)
                            (sql/where where-clause)
                            sql-format)
                (throw (IllegalArgumentException. "Unsupported operation")))]
    (jdbc/execute! tx query)))

(defn filter-keys
  "Filters the keys of each map in the vector, keeping only the specified keys."
  [vec-of-maps keys-to-keep]
  (mapv #(select-keys % keys-to-keep) vec-of-maps))

(defn delete-resource [request]
  (let [model-id (to-uuid (get-in request [:path-params :model_id]))
        tx (:tx request)
        models (db-operation tx :select :models [:= :id model-id])
        _ (when-not (seq models)
            (throw (ex-info "Model not found" {:status 404})))

        items (db-operation tx :select :items [:= :model_id model-id])
        attachments (db-operation tx :select :attachments [:= :model_id model-id])
        images (db-operation tx :select :images [:= :target_id model-id])
        _ (when (seq items)
            (throw (ex-info "Referenced items exist" {:status 403})))

        deleted-model (jdbc/execute! tx
                                     (-> (sql/delete-from :models)
                                         (sql/where [:= :id model-id])
                                         (sql/returning :*)
                                         sql-format))
        _ (db-operation tx :delete :images [:= :target_id model-id])

        remaining-attachments (db-operation tx :select :attachments [:= :model_id model-id])
        remaining-images (db-operation tx :select :images [:= :target_id model-id])
        _ (when (or (seq remaining-attachments) (seq remaining-images))
            (throw (ex-info "Referenced attachments or images still exist" {:status 403})))

        result {:deleted_attachments (remove-nil-values (filter-keys attachments [:id :model_id :filename :size]))
                :deleted_images (remove-nil-values (filter-keys images [:id :target_id :filename :size :thumbnail]))
                :deleted_model (remove-nil-values (filter-keys deleted-model [:id :product :manufacturer]))}]

    (if (= 1 (count deleted-model))
      (response result)
      (throw (ex-info "Failed to delete model" {:status 403})))))

; ##################################

(defn patch-resource [req]
  (let [model-id (to-uuid (get-in req [:path-params :model_id]))
        pool-id (to-uuid (get-in req [:path-params :pool_id]))
        tx (:tx req)
        is-cover (-> req :body-params :is_cover)
        image (jdbc/execute-one! tx (-> (sql/select :*)
                                        (sql/from :images)
                                        (sql/where [:= :id (to-uuid is-cover)])
                                        sql-format))]
    (if (nil? image)
      (bad-request {:error "Image not found"})
      (response (jdbc/execute-one! tx (-> (sql/update :models)
                                          (sql/set {:cover_image_id (to-uuid is-cover)})
                                          (sql/where [:= :id model-id])
                                          (sql/returning :id :cover_image_id)
                                          sql-format))))))