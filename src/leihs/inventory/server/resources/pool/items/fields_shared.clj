(ns leihs.inventory.server.resources.pool.items.fields-shared
  (:require
   [clojure.set :as set]
   [clojure.string :as string]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.constants :refer [PROPERTIES_PREFIX]]
   [leihs.inventory.server.resources.pool.cast-helper :refer [parse-to-bigdecimal-or-nil]]
   [leihs.inventory.server.resources.pool.fields.main :as fields]
   [leihs.inventory.server.utils.authorize.main :refer [authorized-role-for-pool]]
   [leihs.inventory.server.utils.coercion.core :refer [instant-to-date-string]]
   [leihs.inventory.server.utils.request-utils :refer [body-params path-params]]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]
   [ring.middleware.accept]))

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
        {pool-id :pool_id item-id :item_id} (path-params request)
        is-patch? (contains? body-params :data)
        item-data (if is-patch?
                    (:data body-params)
                    (split-item-data body-params))
        item-ids (if is-patch?
                   (:ids body-params)
                   (when item-id [item-id]))
        permitted-fields (-> (fields/base-query "item" (keyword role) pool-id)
                             sql-format
                             (->> (jdbc-query tx)))
        permitted-field-ids (->> permitted-fields
                                 (map (comp keyword :id))
                                 set)
        body-keys (-> (if is-patch?
                        (:data body-params)
                        body-params)
                      (dissoc :id)
                      keys
                      set)
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

    (when (seq unpermitted-fields)
      {:error "Unpermitted fields" :unpermitted-fields unpermitted-fields})

    (when (= model-type "Software")
      {:error "Model type 'Software' is not allowed for items"
       :model_id model-id})

    (when (seq item-ids)
      (let [owners (-> (sql/select :id :owner_id)
                       (sql/from :items)
                       (sql/where [:in :id item-ids])
                       sql-format
                       (->> (jdbc/execute! tx))
                       (->> (reduce (fn [m row] (assoc m (:id row) (:owner_id row))) {})))]
        (some (fn [id]
                (let [existing-owner-id (get owners id)
                      new-owner-id (or owner-id existing-owner-id)
                      owner-changing? (and owner-id (not= owner-id existing-owner-id))]
                  (cond
                    ;; Case 1: Trying to change owner to unauthorized value
                    (and owner-changing?
                         (not= (authorized-role-for-pool request new-owner-id)
                               "inventory_manager")
                         (not= new-owner-id pool-id))
                    {:error (format "Unpermitted owner_id for item %s" id)
                     :provided owner-id
                     :expected pool-id
                     :item_id id}

                    ;; Case 2: Trying to modify item with unauthorized owner
                    (and (not owner-changing?)
                         existing-owner-id
                         (not= existing-owner-id pool-id)
                         (not= (authorized-role-for-pool request existing-owner-id)
                               "inventory_manager"))
                    {:error (format "Unauthorized to modify item %s (wrong owner)" id)
                     :item_id id
                     :owner_id existing-owner-id})))
              item-ids)))))

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
