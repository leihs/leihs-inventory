(ns leihs.inventory.server.resources.pool.models.common
  (:require
   [clojure.spec.alpha :as s]
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
  (let [res (jdbc/execute-one!
             tx
             (-> (sql/select :id :target_id :thumbnail :filename :content_type)
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

;; #####################

(defn- allowed-keys-schema [schema-map]
  (map (fn [k]
         (cond
           (instance? schema.core.OptionalKey k) (:k k)
           (instance? schema.core.RequiredKey k) (:k k)
           :else k))
       (keys schema-map)))

;(defn filter-map-by-schema [m spec]
;  (let [keys-set (allowed-keys-schema spec)]
;    (debug "selecting keys from:" m)
;    (debug "using keys:" keys-set)
;    (select-keys m keys-set)))

(defn filter-map-by-schema
  "Filters one or many maps (in a vector) to only include keys defined in the given schema spec."
  [m-or-coll spec]
  (let [keys-set (allowed-keys-schema spec)
        filter-one (fn [m]
                     (debug "selecting keys from:" m)
                     (debug "using keys:" keys-set)
                     (select-keys m keys-set))]
    (cond
      (map? m-or-coll)
      (filter-one m-or-coll)

      (vector? m-or-coll)
      (mapv filter-one m-or-coll)

      :else
      (do
        (debug "⚠️ Expected a map or vector of maps, got:" (type m-or-coll))
        m-or-coll))))

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
