(ns leihs.inventory.server.resources.pool.items.fields-shared
  (:require
   [clojure.set :as set]
   [clojure.string :as string]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.constants :refer [PROPERTIES_PREFIX]]
   [leihs.inventory.server.middlewares.authorize.main :refer [authorized-role-for-pool]]
   [leihs.inventory.server.resources.pool.cast-helper :refer [parse-to-bigdecimal-or-nil]]
   [leihs.inventory.server.resources.pool.fields.main :as fields]
   [leihs.inventory.server.utils.request :refer [body-params path-params]]
   [leihs.inventory.server.utils.schema :refer [instant-to-date-string]]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]
   [ring.middleware.accept]))

(def RETIRED_REASON_REQUIRES_RETIRED "If retired_reason is set then retired must be set as well.")

(defn validate-retired-reason-requires-retired!
  "Throws if item-data has retired_reason set (non-blank) but retired is nil."
  [item-data]
  (when (and (not (string/blank? (str (get item-data :retired_reason))))
             (nil? (get item-data :retired)))
    (throw (ex-info RETIRED_REASON_REQUIRES_RETIRED {:status 400}))))

(defn split-item-data [body-params]
  (let [field-keys (keys body-params)
        properties-keys (filter #(string/starts-with? (name %) PROPERTIES_PREFIX)
                                field-keys)
        item-keys (remove #(string/starts-with? (name %) PROPERTIES_PREFIX)
                          field-keys)
        properties (->> properties-keys
                        (map (fn [k]
                               [(-> k
                                    name
                                    (string/replace
                                     (re-pattern (str "^" PROPERTIES_PREFIX)) "")
                                    keyword)
                                (get body-params k)]))
                        (into {}))
        item-data (select-keys body-params item-keys)]
    {:item-data item-data
     :properties properties}))

(defn validate-field-permissions [request]
  (let [tx (:tx request)
        role (:role (:authenticated-entity request))
        body-params (body-params request)
        {pool-id :pool_id item_id :item_id} (path-params request)
        {:keys [item-data]} (split-item-data body-params)
        existing-owner-id (when item_id
                            (-> (sql/select :owner_id)
                                (sql/from :items)
                                (sql/where [:= :id item_id])
                                sql-format
                                (->> (jdbc/execute-one! tx))
                                :owner_id))
        permitted-fields (-> (fields/base-query "item" (keyword role) pool-id)
                             (cond-> (some-> existing-owner-id
                                             (or (:owner_id item-data))
                                             (not= pool-id))
                               (sql/where fields/not-owner-required))
                             sql-format
                             (->> (jdbc-query tx)))
        permitted-field-ids (->> permitted-fields
                                 (map (comp keyword :id))
                                 set)
        body-keys (-> body-params (dissoc :id :inventory_code :count) keys set)
        unpermitted-fields (set/difference body-keys permitted-field-ids)
        owner-id (:owner_id item-data)
        model-id (:model_id item-data)
        model-data (when model-id
                     (-> (sql/select :type)
                         (sql/from :models)
                         (sql/where [:= :id model-id])
                         sql-format
                         (->> (jdbc/execute-one! tx))))
        model-type (:type model-data)]
    (cond
      (seq unpermitted-fields)
      {:error "Unpermitted fields" :unpermitted-fields unpermitted-fields}

      (= model-type "Software")
      {:error "Model type 'Software' is not allowed for items"
       :model_id model-id}

      (or (and item_id owner-id
               (not= (authorized-role-for-pool request owner-id)
                     "inventory_manager")
               (not= owner-id pool-id))
          (and (not item_id) (not= owner-id pool-id)))
      {:error "Unpermitted owner_id"
       :provided owner-id
       :expected pool-id})))

(defn flatten-properties [item]
  (let [properties (:properties item)
        properties-with-prefix
        (reduce (fn [acc [k v]]
                  (assoc acc (keyword (str PROPERTIES_PREFIX (name k))) v))
                {}
                properties)
        item-without-properties (dissoc item :properties)]
    (merge item-without-properties properties-with-prefix)))

(def in-coercions
  {:retired (fn [v _] (when (true? v) (java.util.Date.)))
   :inventory_pool_id (fn [v i] (or v (:owner_id i)))
   :price (fn [v _] (parse-to-bigdecimal-or-nil v))})

(def out-coercions
  {:retired (fn [v _] (some? v))
   :last_check (fn [v _] (instant-to-date-string v))
   :invoice_date (fn [v _] (instant-to-date-string v))
   :price (fn [v _] (when v (format "%.2f" v)))})

(defn coerce-field-values [item-data c-set]
  (reduce (fn [m [k c-fn]]
            (if (contains? item-data k)
              (update m k c-fn item-data)
              m))
          item-data
          c-set))
