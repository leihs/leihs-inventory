(ns leihs.inventory.server.resources.pool.fields.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.db :as db]
   [leihs.inventory.server.constants :refer [PROPERTIES_PREFIX]]
   [leihs.inventory.server.resources.pool.attachments.main :as attachments]
   [leihs.inventory.server.resources.pool.buildings.main :as buildings]
   [leihs.inventory.server.resources.pool.inventory-code :as inv-code]
   [leihs.inventory.server.resources.pool.inventory-pools.main :as pools]
   [leihs.inventory.server.resources.pool.models.main :as models]
   [leihs.inventory.server.resources.pool.rooms.main :as rooms]
   [leihs.inventory.server.resources.pool.software.main :as software]
   [leihs.inventory.server.resources.pool.suppliers.main :as suppliers]
   [leihs.inventory.server.utils.debug :refer [log-by-severity]]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.request-utils :refer [path-params
                                                       query-params]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [response]]
   [taoensso.timbre :as timbre :refer [debug spy]]))

(def ERROR_GET "Failed to get fields")

(def excluded-keys #{:active :data :dynamic})
(def common-data-keys #{:default
                        :exclude_from_submit
                        :group
                        :label
                        :required
                        :type
                        :visibility_dependency_field_id
                        :visibility_dependency_value})
(def type-data-keys
  {:select #{:default :values}
   :checkbox #{:values}
   :radio #{:default :values}
   :date #{:default}
   :autocomplete #{:values :values_url :values_dependency_field_id}
   :autocomplete-search #{:form_name :value_attr :search_attr :search_path}
   :composite #{:data_dependency_field_id}})

(def keys-hooks
  (let [pools-hook (fn [f & {:keys [tx resource-id user-id pool]}]
                     (-> f
                         (assoc :values
                                (-> pools/base-query (dissoc :select)
                                    (sql/select [:id :value] [:name :label] :is_active)
                                    (cond-> resource-id
                                      (pools/for-inventory-manager user-id))
                                    (sql/order-by [:is_active :desc] :name)
                                    sql-format
                                    (->> (jdbc/query tx))))
                         (assoc :default {:value (:id pool),
                                          :label (:name pool)})))]
    {:building_id (fn [f & {:keys [tx]}]
                    (assoc f :values
                           (-> buildings/base-query (dissoc :select)
                               (sql/select [:id :value] [:name :label])
                               sql-format (->> (jdbc/query tx)))))
     :supplier_id (fn [f & {:keys [tx]}]
                    (assoc f :values
                           (-> suppliers/base-query (dissoc :select)
                               (sql/select [:id :value] [:name :label])
                               sql-format
                               (->> (jdbc/query tx)))))
     :inventory_pool_id pools-hook
     :owner_id pools-hook
     :room_id (fn [f & {:keys [pool]}]
                (assoc f :values_url
                       (str "/inventory/" (:id pool) "/rooms/")))
     :model_id (fn [f & {:keys [pool]}]
                 (assoc f :values_url
                        (str "/inventory/" (:id pool) "/models/?type=model")))
     :software_model_id (fn [f & {:keys [pool]}]
                          (assoc f :values_url
                                 (str "/inventory/" (:id pool) "/software/")))
     :retired (fn [f & _] (assoc f :default false))
     :inventory_code (fn [f & {:keys [tx pool resource-id]}]
                       (if resource-id
                         f
                         (assoc f :default (inv-code/propose tx (:id pool)))))}))

(defn handle-default [tx field-id value item-data pool-id]
  (let [hooks {:supplier_id suppliers/get-by-id
               :inventory_pool_id pools/get-by-id
               :owner_id pools/get-by-id
               :room_id rooms/get-by-id
               :model_id models/get-by-id
               :software_model_id software/get-by-id}
        item-id (:id item-data)]
    (if (and (uuid? value) (contains? hooks field-id))
      (let [res ((hooks field-id) tx value)]
        {:value (:id res), :label (:name res)})
      (case field-id
        :attachments (let [as (attachments/get-by-item-id tx item-id)]
                       (when (seq as)
                         (map #(hash-map :content_type (:content_type %)
                                         :filename (:filename %)
                                         :id (:id %)
                                         :url (str "/inventory/" pool-id
                                                   "/items/" item-id
                                                   "/attachments/" (:id %)))
                              as)))
        :building_id (let [room-id (:room_id item-data)
                           building (buildings/get-by-room-id tx room-id)]
                       {:value (:id building), :label (:name building)})
        :retired (some? value)
        value))))

(defn target-type-expr [ttype]
  (if (= ttype "package")
    [:= [:raw "fields.data->>'forPackage'"] "true"]
    (let [ttype-expr [:raw "fields.data->>'target_type'"]]
      [:or
       [:is-null ttype-expr]
       [:= ttype-expr ttype]])))

(def not-owner-required
  [:or
   [:is-null [:raw "fields.data->'permissions'->>'owner'"]]
   [:= [:raw "fields.data->'permissions'->>'owner'"] "false"]])

(defn min-req-role-expr [min-req-role]
  [:in
   [:raw "fields.data->'permissions'->>'role'"]
   (case min-req-role
     :lending_manager ["lending_manager"]
     :inventory_manager ["lending_manager" "inventory_manager"])])

(defn base-query [ttype role pool-id]
  (-> (sql/select :fields.*)
      (sql/from :fields)
      (sql/left-join :disabled_fields
                     [:and
                      [:= :disabled_fields.field_id :fields.id]
                      [:= :disabled_fields.inventory_pool_id pool-id]])
      (sql/where [:= :fields.active true])
      (sql/where [:= :disabled_fields.id nil])
      (sql/where (target-type-expr ttype))
      (sql/where (min-req-role-expr (keyword role)))))

(defn transform-field-data [field & {:keys [tx pool user-id]
                                     {resource-id :id :as item-data} :item-data}]
  (let [base (reduce (fn [f data-key]
                       (assoc f data-key
                              (-> f
                                  (get-in [:data data-key])
                                  (cond-> (= data-key :required) boolean))))
                     field
                     common-data-keys)]
    (-> base

        ;; Add protected attribute for owner-only fields
        (cond-> resource-id
          (#(if (and (-> %
                         (get-in [:data :permissions :owner])
                         true?)
                     (not= (:owner_id item-data) (:id pool)))
              (assoc %
                     :protected true
                     :protected_reason "editable for owner only")
              (assoc % :protected false))))

        ;; Merge in type-specific data keys
        (merge (select-keys (:data base)
                            (-> base :type keyword type-data-keys)))

        ;; Remove excluded keys
        (#(apply dissoc % excluded-keys))

        ;; Apply hooks for specific keys
        (#(if-let [hook-fn (keys-hooks (-> % :id keyword))]
            (hook-fn % :tx tx :pool pool
                     :resource-id resource-id :user-id user-id)
            %))

        ;; Remove all keys with nil values
        (->> (remove (fn [[_ v]] (nil? v)))
             (into {})))))

(defn get-item-data [tx pool-id item-id]
  (when-let [item (-> (sql/select :*)
                      (sql/from :items)
                      (sql/where [:= :id item-id])
                      (sql/where [:or
                                  [:= :owner_id pool-id]
                                  [:= :inventory_pool_id pool-id]])
                      sql-format
                      (->> (jdbc/query tx))
                      first)]
    (let [properties (:properties item)
          item-without-properties (dissoc item :properties)
          properties-with-prefix
          (reduce (fn [acc [k v]]
                    (assoc acc (keyword (str PROPERTIES_PREFIX (name k))) v))
                  {}
                  properties)]
      (merge item-without-properties properties-with-prefix))))

(defn handle-item-defaults [tx field item-data pool-id]
  (let [field-id (keyword (:id field))
        value (get item-data field-id)]
    (assoc field :default
           (handle-default tx field-id value item-data pool-id))))

(defn index-resources
  [{:keys [tx] {:keys [role] user-id :id} :authenticated-entity :as request}]
  (try
    (let [{:keys [target_type resource_id]} (query-params request)
          {:keys [pool_id]} (path-params request)
          pool (pools/get-by-id tx pool_id)
          query (base-query target_type role pool_id)
          fields (jdbc/query tx (sql-format query))
          item-data (get-item-data tx pool_id resource_id)
          transformed-fields (map #(transform-field-data % :tx tx
                                                         :pool pool
                                                         :user-id user-id
                                                         :item-data item-data)
                                  fields)
          fields-with-defaults (if item-data
                                 (map #(handle-item-defaults tx % item-data pool_id)
                                      transformed-fields)
                                 transformed-fields)]
      (response {:fields (vec fields-with-defaults)}))
    (catch Exception e
      (log-by-severity ERROR_GET e)
      (exception-handler request ERROR_GET e))))
