(ns leihs.inventory.server.resources.models.form.items.model-by-pool-form-fetch
  (:require
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [honey.sql :as sq :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.core :refer [presence]]
   [leihs.inventory.server.resources.models.form.license.queries :refer [model-query
                                                                         inventory-manager-license-subquery
                                                                         lending-manager-license-subquery
                                                                         inventory-manager-item-subquery
                                                                         item-base-query
                                                                         lending-manager-item-subquery
                                                                         license-base-query]]
   [leihs.inventory.server.resources.models.helper :refer [fetch-latest-inventory-code]]
   [leihs.inventory.server.resources.models.queries :refer [accessories-query
                                                            attachments-query
                                                            entitlements-query
                                                            item-query
                                                            model-links-query
                                                            properties-query]]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.core :refer [single-entity-get-request?]]
   [leihs.inventory.server.utils.helper :refer [convert-map-if-exist]]
   [leihs.inventory.server.utils.pagination :refer [fetch-pagination-params
                                                    pagination-response
                                                    create-pagination-response]]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :as jdbco]
   [ring.util.response :as response :refer [bad-request]]
   [taoensso.timbre :refer [error]])
  (:import [java.time LocalDateTime]
           [java.time.format DateTimeFormatter]
           [java.util UUID]))

(defn get-current-timestamp []
  (let [current-timestamp (LocalDateTime/now)
        formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss")]
    (.format current-timestamp formatter)))

(defn rename-keys
  "Renames keys in a map based on a provided key mapping.
   `key-map` is a map where the keys are old keys and the values are new keys."
  [m key-map]
  (reduce
   (fn [acc [old-key new-key]]
     (if (contains? m old-key)
       (assoc acc new-key (get m old-key))
       acc))
   (apply dissoc m (keys key-map))
   key-map))

(defn subquery-by-role [roles-for-pool]
  (let [roles (if (set? roles-for-pool)
                roles-for-pool
                (:roles roles-for-pool))
        subquery (cond
                   (contains? roles :inventory_manager) inventory-manager-item-subquery
                   (contains? roles :lending_manager) lending-manager-item-subquery
                   :else nil)]

    (when-not subquery
      (throw (Exception. "invalid role for the requested pool")))
    subquery))

(defn fetch-items-handler-by-pool-form [request]
  (let [current-timestamp (get-current-timestamp)

        is-fetch-item-request (-> request :path-params :item_id boolean)
        p (println ">o> abc.is-fetch-item-request" is-fetch-item-request)

        tx (get-in request [:tx])
        roles-for-pool (:roles-for-pool request)
        item-id (to-uuid (get-in request [:path-params :item_id]))
        model-id (to-uuid (get-in request [:path-params :model_id]))
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        subquery (subquery-by-role roles-for-pool)]

    (try
      (let [query (-> (sql/select :*)
                      subquery
                      sql-format)

            fields (jdbc/execute! tx query)
            ;; TODO: is quantity not defined in db?
            fields (if is-fetch-item-request
                     fields
                     (conj fields {:active true
                                   :data {:type "text"
                                          :group "Inventory"
                                          :label "Anzahl"
                                          :values "1"
                                          :value "1"}
                                   :attribute "quantity"
                                   :default false
                                   :forPackage true
                                   :group "Inventory"
                                   :group_default "Inventory"
                                   :id "quantity"
                                   :label "Anzahl"
                                   :owner nil
                                   :position 13
                                   :role nil
                                   :role_default ""
                                   :target nil
                                   :target_default ""}))

            model-result (if model-id
                           ;; Fetch model data
                           (let [model-query (-> (item-base-query item-id model-id pool-id) sql-format)
                                 model-result (jdbc/execute-one! tx model-query)
                                 model-result (when model-result
                                                (let [model-result (assoc model-result
                                                                          :product {:name (:product_name model-result)
                                                                                    :model_id (:model_id model-result)})

                                                      supplier_name (:supplier_name model-result)
                                                      supplier_id (:supplier_id model-result)
                                                      supplier-data (if (some? supplier_id) {:name supplier_name
                                                                                             :supplier_id supplier_id}
                                                                        nil)
                                                      model-result (assoc model-result :supplier supplier-data)
                                                      attachments (jdbc/execute! tx
                                                                                 (-> (sql/select :id :filename :content_type :size)
                                                                                     (sql/from :attachments)
                                                                                     (sql/where [:= :item_id item-id])
                                                                                     sql-format))

                                                      p (println ">o> abc.attachments" attachments)
                                                      model-result (assoc model-result :attachments attachments)
                                                      model-result (rename-keys model-result {:item_version :version})
                                                      retired (not (nil? (:retired model-result)))
                                                      model-result (assoc model-result :retired retired)]
                                                  model-result))]
                             model-result)

                           ;; Fetch default
                           (let [responsible_department "b582d569-05c1-5d60-aeb8-b67a10bb2957"
                                 {:keys [next-code]} (fetch-latest-inventory-code tx pool-id)]
                             {:inventory_pool_id pool-id
                              :responsible_department responsible_department
                              :quantity 1
                              :inventory_code next-code}))]

        (if model-result
          (response/response {:data model-result :fields fields})
          (response/status (response/response {:error "Failed to fetch item"
                                               :details "No data found"}) 404)))
      (catch Exception e
        (error "Failed to fetch item" (.getMessage e))
        (bad-request {:error "Failed to fetch item" :details (.getMessage e)})))))
