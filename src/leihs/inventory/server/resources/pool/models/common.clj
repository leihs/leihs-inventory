(ns leihs.inventory.server.resources.pool.models.common
  (:require
   [clojure.spec.alpha :as s]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.middlewares.debug :refer [log-by-severity]]
   [next.jdbc :as jdbc]
   [taoensso.timbre :refer [debug]]))

(def ERROR_FETCH_IMAGES_BY_TARGET_IDS "Failed to fetch images by target-ids")
(def ERROR_FETCH_IMAGES_BY_COVER_IDS "Failed to fetch images by cover-ids")

(defn remove-nil-values
  "Removes all nil values from a map or a vector of maps."
  [x]
  (cond
    (map? x) (into {} (remove (comp nil? val) x))
    (sequential? x) (mapv remove-nil-values x)
    :else x))

(defn- fetch-images-by-target-ids [tx target-ids]
  (when (seq target-ids)
    (try
      (let [query (-> (sql/select :id :target_id :thumbnail :filename :content_type)
                      (sql/from :images)
                      (sql/where [:in :target_id target-ids])
                      (sql/order-by [:thumbnail :asc])
                      sql-format)
            images (jdbc/execute! tx query)]
        (->> images
             (group-by :target_id)
             (map (fn [[target-id imgs]]
                    [target-id (first imgs)]))
             (into {})))
      (catch Exception e
        (log-by-severity ERROR_FETCH_IMAGES_BY_TARGET_IDS e)
        (throw e)))))

(defn- fetch-images-by-cover-ids [tx cover-ids]
  (when (seq cover-ids)
    (try
      (let [cover-ids-set (set cover-ids)
            query (-> (sql/select :id :parent_id :thumbnail :filename :content_type)
                      (sql/from :images)
                      (sql/where [:or
                                  [:in :id cover-ids]
                                  [:in :parent_id cover-ids]])
                      (sql/order-by [:thumbnail :asc])
                      sql-format)
            images (jdbc/execute! tx query)]
        (reduce (fn [acc image]
                  (let [img-id (:id image)
                        parent-id (:parent_id image)]
                    (cond-> acc
                      (contains? cover-ids-set img-id)
                      (update img-id (fn [existing]
                                       (or existing image)))
                      (contains? cover-ids-set parent-id)
                      (update parent-id (fn [existing]
                                          (or existing image))))))
                {}
                images))
      (catch Exception e
        (log-by-severity ERROR_FETCH_IMAGES_BY_COVER_IDS e)
        (throw e)))))

(defn- enrich-model-with-image [model images-by-cover-id images-by-target-id]
  (if-let [image (cond
                   (:cover_image_id model) (get images-by-cover-id (:cover_image_id model))
                   :else (get images-by-target-id (:id model)))]
    (assoc model
           :image_id (:id image)
           :content_type (:content_type image))
    model))

(defn fetch-thumbnails-for-ids [tx model-cover-ids]
  (if (empty? model-cover-ids)
    []
    (let [models-with-cover (filter :cover_image_id model-cover-ids)
          models-without-cover (remove :cover_image_id model-cover-ids)

          images-by-target-id (fetch-images-by-target-ids
                               tx
                               (map :id models-without-cover))

          images-by-cover-id (fetch-images-by-cover-ids
                              tx
                              (map :cover_image_id models-with-cover))]

      (vec (map #(enrich-model-with-image % images-by-cover-id images-by-target-id)
                model-cover-ids)))))

;; #####################

(defn- allowed-keys-schema [schema-map]
  (map (fn [k]
         (cond
           (instance? schema.core.OptionalKey k) (:k k)
           (instance? schema.core.RequiredKey k) (:k k)
           :else k))
       (keys schema-map)))

(defn filter-map-by-schema [m spec]
  (let [keys-set (allowed-keys-schema spec)]
    (debug "selecting keys from:" m)
    (debug "using keys:" keys-set)
    (select-keys m keys-set)))

;; #####################

(defn- allowed-keys-spec [spec]
  (let [resolved-spec (clojure.spec.alpha/get-spec spec)
        spec-form (when resolved-spec (clojure.spec.alpha/form resolved-spec))]
    (cond
      (and (seq? spec-form) (= 'clojure.spec.alpha/keys (first spec-form)))
      (let [args (apply hash-map (rest spec-form))
            req-keys (map #(do (debug "req-un-key:" %) (-> % name keyword)) (get args :req-un))
            opt-keys (map #(do (debug "opt-un-key:" %) (-> % name keyword)) (get args :opt-un))]
        (debug "args:" args)
        (debug "req-keys:" req-keys)
        (debug "opt-keys:" opt-keys)
        (set (concat req-keys opt-keys)))
      (qualified-keyword? spec)
      #{(keyword (name spec))}

      :else
      #{})))

(defn filter-map-by-spec [m spec]
  (let [keys-set (allowed-keys-spec spec)]
    (debug "selecting keys from:" m "using keys:" keys-set)
    (select-keys m keys-set)))

(defn filter-and-coerce-by-spec
  "Filter by spec, remove-nil-values is optional"
  ([models spec]
   (filter-and-coerce-by-spec models spec false))

  ([models spec remove-nil-values?]
   (let [models (if remove-nil-values? (remove-nil-values models) models)]
     (mapv #(filter-map-by-spec % spec) models))))

(defn model->enrich-with-image-attr
  [pool-id]
  (fn [{:keys [id image_id content_type] :as m}]
    (cond-> m
      image_id (assoc :url (str "/inventory/" pool-id "/models/" id "/images/" image_id)
                      :content_type content_type))))
