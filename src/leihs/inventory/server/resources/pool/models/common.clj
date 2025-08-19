(ns leihs.inventory.server.resources.pool.models.common
  (:require
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [next.jdbc :as jdbc]
   [taoensso.timbre :refer [debug]]))

(defn remove-nil-values
  "Removes all nil values from a map or a vector of maps."
  [x]
  (cond
    (map? x) (into {} (remove (comp nil? val) x))
    (sequential? x) (mapv remove-nil-values x)
    :else x))

(defn- get-one-thumbnail-query [tx {:keys [id cover_image_id] :as model-cover-id}]
  (let [res (jdbc/execute-one! tx (-> (sql/select :id :target_id :thumbnail :filename :content_type)
                                      (sql/from :images)
                                      (cond-> (nil? cover_image_id) (sql/where [:= :target_id id]))
                                      (cond-> (not (nil? cover_image_id)) (sql/where [:or
                                                                                      [:= :id cover_image_id]
                                                                                      [:= :parent_id cover_image_id]]))
                                      (sql/order-by [:thumbnail :asc])
                                      sql-format))

        data (if res (assoc model-cover-id
                            :image_id (:id res)
                            :content_type (:content_type res))
                 model-cover-id)]
    data))

(defn fetch-thumbnails-for-ids [tx model-cover-ids]
  (vec (map #(get-one-thumbnail-query tx %) model-cover-ids)))

(defn create-url [pool_id model_id type cover_image_id]
  (str "/inventory/" pool_id "/models/" model_id "/images/" cover_image_id))

(defn apply-cover-image-urls [models thumbnails pool_id]
  (vec
   (map-indexed
    (fn [idx model]
      (let [cover-image-id (:cover_image_id model)
            origin_table (:origin_table model)
            thumbnail-id (-> (filter #(= (:target_id %) (:id model)) thumbnails)
                             first
                             :id)]
        (cond-> model
          (and (= "models" origin_table) cover-image-id)
          (assoc :url (create-url pool_id (:id model) "images" cover-image-id)))))
    models)))

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
        _ (debug "resolved-spec:" resolved-spec)
        spec-form (when resolved-spec (clojure.spec.alpha/form resolved-spec))
        _ (debug "spec-form:" spec-form)]
    (cond
      (and (seq? spec-form) (= 'clojure.spec.alpha/keys (first spec-form)))
      (let [args (apply hash-map (rest spec-form))
            _ (debug "args:" args)
            req-keys (map #(do (debug "req-un-key:" %) (-> % name keyword)) (get args :req-un))
            opt-keys (map #(do (debug "opt-un-key:" %) (-> % name keyword)) (get args :opt-un))]
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
