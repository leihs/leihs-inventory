(ns leihs.inventory.server.resources.fields.main
  (:require
   [clojure.set]
   [honey.sql :as sq]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.core :refer [single-entity-get-request?]]
   [leihs.inventory.server.utils.pagination :refer [fetch-pagination-params]]
   [leihs.inventory.server.utils.pagination :refer [pagination-response]]
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
           {:keys [role owner]} (-> request query-params)
           {:keys [page size]} (fetch-pagination-params request)
           user-id (:id (:authenticated-entity request))
           inventory-access-base-query (-> (sql/from :inventory_pools)
                                           (sql/where [:= :inventory_pools.is_active true]))

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
           ;p (println ">o> res" res)

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
                                         (sql/where [:= :f.active true])) :subquery])

                          (cond-> group_id (sql/where [:= :subquery.id group_id]))

                          (cond-> (and (some? role) (not (= "customer" role)))
                            (sql/where [:or
                                        [:= :subquery.role role]
                                        [:is :subquery.role nil]]))

                          (cond-> (and (some? owner) (not (= "customer" role)))
                            (sql/where [:= :subquery.owner owner]))
                          ;  (and (nil? with-pagination?) (valid-get-request? request)) (pagination-response request base-query)
                          ;  with-paginat (sql/where [:= :subquery.owner owner]))

                          (cond-> (= "customer" role)
                            (sql/where [:or
                                        [:not [:in :subquery.role ["inventory_manager" "lending_manager" "group_manager"]]]
                                        [:is :subquery.role nil]])))]

       (cond
         (and (nil? with-pagination?) (single-entity-get-request? request)) (pagination-response request base-query)
         with-pagination? (pagination-response request base-query)
         :else (jdbc/query tx (-> base-query sql-format))))

     (catch Exception e
       (error "Failed to get supplier(s)" e)
       (bad-request {:error "Failed to get supplier(s)" :details (.getMessage e)})))))

(defn get-form-fields-with-pagination-handler [request]
  (response (get-form-fields request true)))

(defn get-form-fields-handler [request]
  (let [result (get-form-fields request)]
    (response result)))
