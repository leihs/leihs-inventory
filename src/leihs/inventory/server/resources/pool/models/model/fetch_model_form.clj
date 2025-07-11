(ns leihs.inventory.server.resources.pool.models.model.fetch-model-form
  (:require
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [honey.sql :as sq :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [clojure.spec.alpha :as sa]
   [leihs.inventory.server.resources.pool.models.coercion :as co]

   [leihs.inventory.server.resources.pool.common :refer [remove-nil-entries-fnc]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]

   [leihs.inventory.server.resources.pool.models.common :refer [apply-cover-image-urls  fetch-thumbnails-for-ids
                                                                remove-nil-values]]

   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]])
  (:import [java.time LocalDateTime]
           [java.util UUID]))




(defn allowed-keys [spec]
  (let [resolved-spec (sa/get-spec spec)
        _ (println "resolved-spec:" resolved-spec)
        spec-form (when resolved-spec (sa/form resolved-spec))
        _ (println "spec-form:" spec-form)]
    (if (and (seq? spec-form) (= 'clojure.spec.alpha/keys (first spec-form)))
      (let [args (apply hash-map (rest spec-form))
            _ (println "args:" args)
            req-keys (map #(keyword (name %)) (get args :req-un))
            opt-keys (map #(keyword (name %)) (get args :opt-un))]
        (println "req-keys:" req-keys)
        (println "opt-keys:" opt-keys)
        (set (concat req-keys opt-keys)))
      (do
        (println "Not a s/keys spec form!")
        #{}))))


(defn filter-map-by-spec [m spec]
  (let [keys-set (allowed-keys spec)]
    (println "selecting keys from:" m "using keys:" keys-set)
    (select-keys m keys-set)))


(defn filter-and-coerce-by-spec
  [models spec]
  (->> models
    remove-nil-values                ; removes nil values from inside maps (if your fn is like that)
    ;(remove nil?)                    ; removes nil maps
    (mapv #(filter-map-by-spec % spec))))


(defn select-entries [tx table columns where-clause]
  (jdbc/execute! tx
                 (-> (apply sql/select columns)
                     (sql/from table)
                     (sql/where where-clause)
                     sql-format)))

(defn fetch-attachments [tx model-id pool-id]

     (let [

  attachments (->> (select-entries tx :attachments [:id :filename] [:= :model_id model-id])
       (map #(assoc % :url (str "/inventory/" pool-id "/models/" model-id "/attachments/" (:id %)))))
              ]
   (filter-and-coerce-by-spec attachments  ::co/attachment)

       )



  )



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

        ;p (println ">o> abc.images-with-urls" images-with-urls)
        ;p (println ">o> abc ????1" (filter-map-by-spec (first images-with-urls) ::co/image))
        ;p (println ">o> abc ????2" ::co/image)
        ;filtered-images (mapv #(filter-map-by-spec % ::co/image) images-with-urls)



        filtered-images (filter-and-coerce-by-spec images-with-urls  ::co/image)

        p (println ">o> abc.filtered-images" filtered-images)
        ]
    filtered-images))



(defn fetch-accessories [tx model-id]
  (let [query (-> (sql/select :a.id :a.name)
                  (sql/from [:accessories :a])
                  (sql/left-join [:accessories_inventory_pools :aip] [:= :a.id :aip.accessory_id])
                  (sql/where [:= :a.model_id model-id])
                  (sql/order-by :a.name)
                  sql-format)
        accessories (jdbc/execute! tx query)        ]
    (mapv #(filter-map-by-spec % ::co/accessory) accessories)    ))







(defn fetch-compatibles [tx model-id pool-id]
  (let [query (-> (sql/select :mm.id :mm.product :mm.version ["models" :origin_table] :mm.cover_image_id)
                  (sql/from [:models_compatibles :mc])
                  (sql/left-join [:models :mm] [:= :mc.compatible_id :mm.id])
                  (sql/where [:= :mc.model_id model-id])
                  sql-format)
        models (jdbc/execute! tx query)

        p (println ">o> abc.models1" models)

        models (->> models
          (fetch-thumbnails-for-ids tx)
           (map (fn [m]
                 (if-let [image-id (:image_id m)]
                   (assoc m :url (str "/inventory/" pool-id "/models/" (:id m) "/images/" image-id))
                   m))))
        p (println ">o> abc.models2" models)
        p (println ">o> abc.models2" (type models))



        models (remove-nil-values models)
        models (mapv #(filter-map-by-spec % ::co/compatible) models)

        p (println ">o> abc.models3" models)
        ;model-cover-ids (->> models (keep :id :cover_image_id) vec)
        ;ids (mapv :id models)
        ;thumbnails (fetch-thumbnails-for-ids tx ids)
        ;models (apply-cover-image-urls models thumbnails pool-id)

    ;models (map #(dissoc % [:origin_table :cover_image_id]) models)
    ;    p (println ">o> abc.models4" models)
        ]
    models
    ))

(defn fetch-properties [tx model-id]
  (select-entries tx :properties [:id :key :value] [:= :model_id model-id]))

(defn fetch-entitlements [tx model-id]
  (let [query (-> (sql/select :e.id :e.quantity :eg.name [:eg.id :group_id])
                  (sql/from [:entitlements :e])
                  (sql/join [:entitlement_groups :eg] [:= :e.entitlement_group_id :eg.id])
                  (sql/where [:= :e.model_id model-id])
                  sql-format)

        entitlements (jdbc/execute! tx query)

        ]

  (filter-and-coerce-by-spec entitlements :json/entitlement)


  ))

(defn fetch-categories [tx model-id]
  (let [category-type "Category"
        query (-> (sql/select :mg.id :mg.type :mg.name)
                  (sql/from [:model_groups :mg])
                  (sql/left-join [:model_links :ml] [:= :mg.id :ml.model_group_id])
                  (sql/where [:ilike :mg.type (str category-type)])
                  (sql/where [:= :ml.model_id model-id])
                  (sql/order-by :mg.name)
                  sql-format)
    categories (jdbc/execute! tx query)
        ]

     (filter-and-coerce-by-spec categories  ::co/category)


    ))

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
