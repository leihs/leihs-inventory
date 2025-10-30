(ns leihs.inventory.server.resources.pool.fields.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.db :as db]
   [leihs.inventory.server.constants :refer [PROPERTIES_PREFIX]]
   [leihs.inventory.server.resources.pool.buildings.main :as buildings]
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
  (let [pools-hook (fn [tx _ f]
                     (assoc f :values
                            (-> pools/base-query (dissoc :select)
                                (sql/select [:id :value] [:name :label] :is_active)
                                (sql/order-by [:is_active :desc] :name)
                                sql-format
                                (->> (jdbc/query tx)))))]
    {:building_id (fn [tx _ f]
                    (assoc f :values
                           (-> buildings/base-query (dissoc :select)
                               (sql/select [:id :value] [:name :label])
                               sql-format (->> (jdbc/query tx)))))
     :supplier_id (fn [tx _ f]
                    (assoc f :values
                           (-> suppliers/base-query (dissoc :select)
                               (sql/select [:id :value] [:name :label])
                               sql-format
                               (->> (jdbc/query tx)))))
     :inventory_pool_id pools-hook
     :owner_id pools-hook
     :room_id (fn [_ pool f]
                (assoc f :values_url
                       (str "/inventory/" (:id pool) "/rooms/")))
     :model_id (fn [_ pool f]
                 (assoc f :values_url
                        (str "/inventory/" (:id pool) "/models/?type=model")))
     :software_model_id (fn [_ pool f]
                          (assoc f :values_url
                                 (str "/inventory/" (:id pool) "/software/")))}))

(def defaults-hooks
  {:building_id buildings/get-by-id
   :supplier_id suppliers/get-by-id
   :inventory_pool_id pools/get-by-id
   :owner_id pools/get-by-id
   :room_id rooms/get-by-id
   :model_id models/get-by-id
   :software_model_id software/get-by-id})

(defn target-type-expr [ttype]
  (if (= ttype "package")
    [:= [:raw "fields.data->>'forPackage'"] "true"]
    (let [ttype-expr [:raw "fields.data->>'target_type'"]]
      [:or
       [:is-null ttype-expr]
       [:= ttype-expr ttype]])))

(defn req-owner?-expr [true-or-false]
  [:in
   [:raw "fields.data->'permissions'->>'owner'"]
   (case true-or-false
     true ["true" "false"]
     false ["false"])])

(defn min-req-role-expr [min-req-role]
  [:in
   [:raw "fields.data->'permissions'->>'role'"]
   (case min-req-role
     :lending_manager ["lending_manager"]
     :inventory_manager ["lending_manager" "inventory_manager"])])

(defn base-query [ttype role]
  (-> (sql/select :*)
      (sql/from :fields)
      (sql/where [:= :fields.active true])
      (sql/where (target-type-expr ttype))
      (sql/where (min-req-role-expr (keyword role)))))

(defn transform-field-data [tx pool field]
  (let [base (reduce (fn [f data-key]
                       (assoc f data-key
                              (-> f
                                  (get-in [:data data-key])
                                  (cond-> (= data-key :required) boolean))))
                     field
                     common-data-keys)]
    (-> base
        ;; Merge in type-specific data keys
        (merge (select-keys (:data base)
                            (-> base :type keyword type-data-keys)))
        ;; Remove excluded keys
        (#(apply dissoc % excluded-keys))
        ;; Apply hooks for specific keys
        (#(if-let [hook-fn (keys-hooks (-> % :id keyword))]
            (hook-fn tx pool %)
            %))
        ;; Remove all keys with nil values
        (->> (remove (fn [[_ v]] (nil? v)))
             (into {})))))

(defn get-item-data [tx pool-id item-id]
  (let [item (-> (sql/select :*)
                 (sql/from :items)
                 (sql/where [:= :id item-id])
                 (sql/where [:or
                             [:= :owner_id pool-id]
                             [:= :inventory_pool_id pool-id]])
                 sql-format
                 (->> (jdbc/query tx))
                 first)
        properties (:properties item)
        item-without-properties (dissoc item :properties)
        properties-with-prefix
        (reduce (fn [acc [k v]]
                  (assoc acc (keyword (str PROPERTIES_PREFIX (name k))) v))
                {}
                properties)]
    (merge item-without-properties properties-with-prefix)))

(defn merge-item-defaults [tx item-data field]
  (let [field-id (keyword (:id field))
        value (get item-data field-id)]
    (if (some? value)
      (assoc field
             :default
             (if (uuid? value)
               (let [res ((defaults-hooks field-id) tx value)]
                 {:value (:id res), :label (:name res)})
               value))
      field)))

(defn index-resources
  [{:keys [tx] {:keys [role]} :authenticated-entity :as request}]
  (debug request)
  (try
    (let [{:keys [target_type resource_id]} (query-params request)
          {:keys [pool_id]} (path-params request)
          pool (pools/get-by-id tx pool_id)
          query (base-query target_type role)
          fields (jdbc/query tx (sql-format query))
          item-data (get-item-data tx pool_id resource_id)
          transformed-fields (map (partial transform-field-data tx pool) fields)
          fields-with-defaults
          (if item-data
            (map (partial merge-item-defaults tx item-data) transformed-fields)
            transformed-fields)]
      (response {:fields (vec fields-with-defaults)}))
    (catch Exception e
      (log-by-severity ERROR_GET e)
      (exception-handler request ERROR_GET e))))

(comment
  (let [tx (db/get-ds)
        ttype "item"
        role :lending_manager
        pool-id "0a78a94e-f545-4a67-b42f-86f716fcf764"]
    (-> (base-query ttype role)
        (sql-format :inline true)
        (->> (jdbc/query tx))
        (->> (map (partial transform-field-data tx pool-id)))
        (->> (map #(select-keys % [:id :type :default])))
        count)))
