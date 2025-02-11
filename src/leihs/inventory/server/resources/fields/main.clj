(ns leihs.inventory.server.resources.fields.main
  (:require
   [clojure.set]
   [honey.sql :as sq]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.core :refer [single-entity-get-request?]]
   [leihs.inventory.server.utils.pagination :refer [pagination-response fetch-pagination-params fetch-pagination-params-raw]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]]
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

           ;; TODO: this should not be used; instead use leihs.inventory.server.resources.models.form.license.queries
           license_keys ["inventory_code"
                         "license_version"
                         "properties_p4u"
                         "properties_license_type"
                         "properties_activation_type"
                         "properties_operating_system"
                         "properties_reference"
                         "properties_installation"
                         "properties_procured_by"
                         "note"
                         "serial_number"
                         "supplier_id"
                         "invoice_date"
                         "properties_maintenance_contract"
                         "retired"
                         "retired_reason"
                         "is_borrowable"
                         "properties_reference"]

           ;; TODO: example
           ;query (-> inventory-access-base-query
           ;        (sql/select :1)
           ;        (sql/from :fields)
           ;        (sql/where
           ;          [:or
           ;           [:exists (user-direct-access-right-subquery user-id CUSTOMER-ROLES)]
           ;           [:exists (user-group-access-right-subquery user-id CUSTOMER-ROLES)]])
           ;        sql-format)
           ;res (jdbc/query tx query)

           base-query (-> (sql/select :*)
                          (sql/from [(-> (sql/select :f.id
                                                     :f.data
                                                     [(sq/call :cast
                                                               (sq/call :jsonb_extract_path_text :f.data "permissions" "role")
                                                               :text) :role]
                                                     [(sq/call :cast
                                                               (sq/call :jsonb_extract_path_text :f.data "permissions" "owner")
                                                               :boolean) :owner])
                                         (sql/from [:fields :f])

                                     ;; TODO: in use? should be removed?
                                         (cond-> (and (some? type) (= "license" type))
                                           (sql/where [:in :f.id license_keys]))

                                         (sql/where [:= :f.active true])) :subquery])

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
         :else (pagination-response request base-query)))

     (catch Exception e
       (error "Failed to get supplier(s)" e)
       (bad-request {:error "Failed to get supplier(s)" :details (.getMessage e)})))))

(defn get-form-fields-auto-pagination-handler [request]
  (response (get-form-fields request nil)))

(defn get-form-fields-handler [request]
  (let [result (get-form-fields request)]
    (response result)))
