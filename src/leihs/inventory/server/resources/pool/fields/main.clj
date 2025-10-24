(ns leihs.inventory.server.resources.pool.fields.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.db :as db]
   [leihs.inventory.server.resources.pool.buildings.main :as buildings]
   [leihs.inventory.server.resources.pool.inventory-pools.main :as pools]
   [leihs.inventory.server.resources.pool.suppliers.main :as suppliers]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.helper :refer [log-by-severity]]
   [leihs.inventory.server.utils.request-utils :refer [path-params
                                                       query-params]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [response]]))

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
     :room_id (fn [_ pool-id f]
                (assoc f :values_url
                       (str "/inventory/" pool-id "/rooms")))
     :model_id (fn [_ pool-id f]
                 (assoc f :values_url
                        (str "/inventory/" pool-id "/models/?type=model")))}))

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

(defn base-query [ttype]
  (-> (sql/select :*)
      (sql/from :fields)
      (sql/where [:= :fields.active true])
      (sql/where (target-type-expr ttype))))

(defn transform-field-data [tx pool-id field]
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
            (hook-fn tx pool-id %)
            %))
        ;; Remove all keys with nil values
        (->> (remove (fn [[_ v]] (nil? v)))
             (into {})))))

(defn index-resources [{:keys [tx] :as request}]
  (try
    (let [{:keys [target_type]} (query-params request)
          {:keys [pool_id]} (path-params request)
          query (base-query target_type)
          fields (jdbc/query tx (sql-format query))
          transformed-fields (map (partial transform-field-data tx pool_id) fields)]
      (response {:fields (vec transformed-fields)}))
    (catch Exception e
      (log-by-severity ERROR_GET e)
      (exception-handler request ERROR_GET e))))

(comment
  (let [tx (db/get-ds)]
    (-> (base-query "item")
        (sql-format :inline true)
        (->> (jdbc/query tx))
        (->> (filter #(= (-> % :data :type) "autocomplete-search")))
        (->> (map (partial transform-field-data tx ":pool-id"))))))
