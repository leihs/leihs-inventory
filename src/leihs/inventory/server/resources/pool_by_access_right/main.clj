(ns leihs.inventory.server.resources.pool-by-access-right.main
  (:require
   [clojure.set]
   [clojure.string :as str]
   [clojure.walk :as walk]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.core :refer [presence]]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.helper :refer [convert-to-map snake-case-keys]]
   [next.jdbc.sql :as jdbc]
   [ring.util.response :refer [bad-request response]]
   [taoensso.timbre :refer [error]]))

(defn get-pools-of-user-handler [request]
  (try
    (let [tx (:tx request)
          user-id (or (presence (-> request path-params :user_id))
                      (:id (:authenticated-entity request)))
          select (sql/select :ip.*)
          ;; TODO: remove is_active?
          query (-> (sql/union-all
                     (-> select
                         (sql/from [:users :u])
                         (sql/join [:groups_users :gu] [:= :u.id :gu.user_id])
                         (sql/join [:groups :g] [:= :gu.group_id :g.id])
                         (sql/join [:direct_access_rights :dar] [:= :u.id :dar.user_id])
                         (sql/join [:inventory_pools :ip] [:= :dar.inventory_pool_id :ip.id])
                         (sql/where [:and [:= :u.id user-id] [:= :ip.is_active true]]))
                     (-> select
                         (sql/from [:users :u])
                         (sql/join [:groups_users :gu] [:= :u.id :gu.user_id])
                         (sql/join [:groups :g] [:= :gu.group_id :g.id])
                         (sql/join [:group_access_rights :gar] [:= :g.id :gar.group_id])
                         (sql/join [:inventory_pools :ip] [:= :gar.inventory_pool_id :ip.id])
                         (sql/where [:and [:= :u.id user-id] [:= :ip.is_active true]])))
                    sql-format)
          result (jdbc/query tx query)]
      (response result))
    (catch Exception e
      (error "Failed to get user" e)
      (bad-request {:error "Failed to get user" :details (.getMessage e)}))))

(defn get-pools-access-rights-of-user-query [min-raw user-id access-right-raw]
  (let [min (boolean min-raw)
        access-right (if (and access-right-raw (not (contains? #{"direct_access_rights" "group_access_rights"} access-right-raw)))
                       nil
                       access-right-raw)
        select (if min (sql/select :i.id :i.name) (sql/select :i.is_active :i.name :u.*))
        query (-> select
                  (sql/from [:unified_access_rights :u])
                  (sql/join [:inventory_pools :i] [:= :u.inventory_pool_id :i.id])
                  (sql/where [:= :u.user_id user-id])
                  (cond-> (= access-right "direct_access_rights") (sql/where [:is-not :u.direct_access_right_id nil])
                          (= access-right "group_access_rights") (sql/where [:is-not :u.group_access_right_id nil]))
                  sql-format)]
    query))

(defn get-pools-access-rights-of-user-handler [request]
  (try
    (let [tx (:tx request)
          user-id (or (presence (-> request path-params :user_id))
                      (:id (:authenticated-entity request)))
          min-raw (boolean (-> request query-params :min))
          access-right-raw (-> request query-params :access_rights)
          query (get-pools-access-rights-of-user-query min-raw user-id access-right-raw)
          result (jdbc/query tx query)]
      (response result))
    (catch Exception e
      (error "Failed to get user" e)
      (bad-request {:error "Failed to get user" :details (.getMessage e)}))))


