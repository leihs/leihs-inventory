(ns leihs.inventory.server.resources.fields.main-new
  (:require
   [clojure.set]
   [honey.sql :as sq]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.core :refer [single-entity-get-request?]]
   [leihs.inventory.server.utils.pagination :refer [pagination-response fetch-pagination-params
                                                    fetch-pagination-params-raw]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status header]]
   [taoensso.timbre :refer [error]]))

(defn get-form-fields
  ([request]
   (get-form-fields request false))
  ([request with-pagination?]
   (try
     (let [tx (:tx request)
           pool_id (-> request path-params :pool_id)
           group_id (-> request path-params :field_id)

           {:keys [role owner type]} (-> request query-params)
           {:keys [page size]} (fetch-pagination-params-raw request)
           user-id (:id (:authenticated-entity request))
           inventory-access-base-query (-> (sql/from :inventory_pools)
                                           (sql/where [:= :inventory_pools.is_active true]))

           base-query (-> (sql/select :*)
                          (sql/from [(-> (sql/select :f.id
                                                     [(sq/call :jsonb_extract_path_text :f.data "label") :label]
                                                     :f.active
                                                     :f.position
                                                     [(sq/call :jsonb_extract_path_text :f.data "group") :group]
                                                     [(sq/call :cast (sq/call :jsonb_extract_path_text :f.data "permissions" "role") :text) :role]
                                                     [(sq/call :cast (sq/call :jsonb_extract_path_text :f.data "permissions" "owner") :boolean) :owner]
                                                     :f.data
                                                     :f.dynamic)
                                         (sql/from [:fields :f])
                                         (sql/where [:or
                                                     [:in (sq/call :jsonb_extract_path_text :f.data "group") ["Status" "Invoice Information" "General Information" "Inventory" "Maintenance"]]
                                                     [:= (sq/call :jsonb_extract_path_text :f.data "group") nil]])
                                         (sql/where [:not-in :f.id
                                                     (-> (sql/select :df.field_id)
                                                         (sql/from [:disabled_fields :df])
                                                         (sql/where [:= :df.inventory_pool_id pool_id]))])
                                         (sql/where [:= :f.active true])
                                         (sql/where [:or
                                                     [:in (sq/call :cast (sq/call :jsonb_extract_path_text :f.data "permissions" "role") :text) ["inventory_manager" "lending_manager"]]
                                                     [:= (sq/call :cast (sq/call :jsonb_extract_path_text :f.data "permissions" "role") :text) nil]])
                                         (sql/order-by [(sq/call :jsonb_extract_path_text :f.data "group")] :position)) :subquery])

                          (cond-> group_id (sql/where [:= :subquery.id group_id]))

                          (cond-> (and (some? role) (not (= "customer" role)))
                            (sql/where [:or
                                        [:= :subquery.role role]
                                        [:is :subquery.role nil]]))

                          (cond-> (and (some? owner) (not (= "customer" role)))
                            (sql/where [:= :subquery.owner owner]))

                          (cond-> (= "customer" role)
                            (sql/where [:or
                                        [:not [:in :subquery.role ["inventory_manager" "lending_manager" "group_manager"]]]
                                        [:is :subquery.role nil]])))]

       (cond
         (and (nil? with-pagination?) (nil? page) (nil? size)) (jdbc/query tx (-> base-query sql-format))
         (and (nil? with-pagination?) (single-entity-get-request? request)) (pagination-response request base-query)
         with-pagination? (pagination-response request base-query)
         :else (jdbc/query tx (-> base-query sql-format))))

     (catch Exception e
       (error "Failed to get supplier(s)" e)
       (bad-request {:error "Failed to get supplier(s)" :details (.getMessage e)})))))

(defn get-form-fields-auto-new-pagination-handler [request]
  (let [result (get-form-fields request nil)
        data (if (map? result) (get result :data) result)]
    (if (vector? result)
      (-> (response result)
          (header "Count" (count data)))
      (response result))))
