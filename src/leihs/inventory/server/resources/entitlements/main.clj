(ns leihs.inventory.server.resources.entitlements.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [path-params]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]]))


(defn get-model-links-of-pool-handler [request]
  (try
    (let [tx (:tx request)
          pool_id (-> request path-params :pool_id)
          id (-> request path-params :id)
          ;query (-> (sql/select :eg.id :eg.name [[:count :egdu.user_id] :user_count] :eg.is_verification_required)
          ;        (sql/from [:entitlement_groups :eg])
          ;        (sql/join [:entitlement_groups_direct_users :egdu] [:= :eg.id :egdu.entitlement_group_id])
          ;
          ;        (sql/group-by :edgu.user_id)
          ;        (sql/where [:= :eg.id pool_id]) ; Adjusted to use `:ip.id` as an example
          ;        (cond-> id (sql/where [:= :eg.id id]))
          ;
          ;        (sql/limit 50)
          ;        sql-format)

          ;query (-> (sql/select :eg.id :eg.name [[:count :egdu.user_id] :user_count] :eg.is_verification_required)
          ;query (-> (sql/select :eg.id :eg.name :eg.is_verification_required)
          ;        (sql/from [:entitlement_groups :eg])
          ;        (sql/join [:entitlement_groups_direct_users :egdu] [:= :eg.id :egdu.entitlement_group_id])
          ;        ;(sql/group-by :eg.id :eg.name :eg.is_verification_required) ; Group by entitlement group id to get a count of users per group
          ;        (sql/where [:= :eg.id pool_id])
          ;        (cond-> id (sql/where [:= :eg.id id]))
          ;        (sql/limit 50)
          ;        sql-format)


          query (-> (sql/select :eg.id :eg.name
                      [[:count :egdu.user_id] :user_count]
                      :eg.is_verification_required)
                  (sql/from [:entitlement_groups :eg])
                  (sql/join [:entitlement_groups_direct_users :egdu]
                    [:= :eg.id :egdu.entitlement_group_id])
                  (sql/where [:= :eg.inventory_pool_id pool_id])
                  (cond-> id (sql/where [:= :eg.id id])) ; Add an additional where clause if `id` is provided
                  (sql/group-by :eg.id :eg.name :eg.is_verification_required)
                  (sql/limit 50)
                  sql-format)


          p (println ">o> query" query)


          result (jdbc/query tx query)]
      (response result))
    (catch Exception e
      (error "Failed to get pools of user" e)
      (bad-request {:error "Failed to get pools of user" :details (.getMessage e)}))))
