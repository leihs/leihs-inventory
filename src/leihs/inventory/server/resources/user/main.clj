(ns leihs.inventory.server.resources.user.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.core :refer [presence]]
   [leihs.inventory.server.resources.user.languages :as l]
   [leihs.inventory.server.resources.user.profile :refer [get-current get-navigation get-one get-settings]]
   [leihs.inventory.server.resources.utils.request :refer [path-params]]
   [leihs.inventory.server.utils.helper :refer [convert-to-map]]
   [next.jdbc.sql :as jdbc]
   [ring.util.response :refer [bad-request response]]
   [taoensso.timbre :refer [error]]))

(defn get-pools-of-user-handler [request]
  (try
    (let [tx (:tx request)
          user-id (or (presence (-> request path-params :user_id))
                      (:id (:authenticated-entity request)))
          ;query (-> (sql/select :p.id :p.name :p.description)
          ;          (sql/from [:direct_access_rights :d])
          ;          (sql/join [:users :u] [:= :u.id :d.user_id])
          ;          (sql/join [:inventory_pools :p] [:= :d.inventory_pool_id :p.id])
          ;          (sql/where [:= :u.id user-id])
          ;          sql-format)

          select (sql/select :ip.*)
          query (-> (sql/union-all
                         ;; Query for direct_access_rights
                     (-> select
                         (sql/from [:users :u])
                         (sql/join [:groups_users :gu] [:= :u.id :gu.user_id])
                         (sql/join [:groups :g] [:= :gu.group_id :g.id])
                         (sql/join [:direct_access_rights :dar] [:= :u.id :dar.user_id])
                         (sql/join [:inventory_pools :ip] [:= :dar.inventory_pool_id :ip.id])
                         (sql/where [:and
                                     [:= :u.id user-id]
                                     [:= :ip.is_active true]]))

                         ;; Query for group_access_rights
                     (-> select
                         (sql/from [:users :u])
                         (sql/join [:groups_users :gu] [:= :u.id :gu.user_id])
                         (sql/join [:groups :g] [:= :gu.group_id :g.id])
                         (sql/join [:group_access_rights :gar] [:= :g.id :gar.group_id])
                         (sql/join [:inventory_pools :ip] [:= :gar.inventory_pool_id :ip.id])
                         (sql/where [:and
                                     [:= :u.id user-id]
                                     [:= :ip.is_active true]])))
                    sql-format)

          result (jdbc/query tx query)]
      (response result))
    (catch Exception e
      (error "Failed to get user" e)
      (bad-request {:error "Failed to get user" :details (.getMessage e)}))))

(defn get-user-details-handler [request]
  (try
    (let [tx (:tx request)
          user-id (or (presence (-> request path-params :user_id))
                      (:id (:authenticated-entity request)))
          user-query (-> (sql/select :u.id :u.login :u.email :u.firstname :u.lastname :u.organization :u.is_admin :u.org_id)
                         (sql/from [:users :u])
                         (sql/where [:= :u.id user-id])
                         sql-format)
          user-res (jdbc/query tx user-query)
          auth-query (-> (sql/select :a.id :a.authentication_system_id :a.created_at :a.updated_at)
                         (sql/from [:users :u])
                         (sql/join [:authentication_systems_users :a] [:= :u.id :a.user_id])
                         (sql/where [:= :u.id user-id])
                         sql-format)
          auth-res (jdbc/query tx auth-query)
          token-res (vec (map #(dissoc % :token_hash :user_id :description)
                              (jdbc/query tx (-> (sql/select :t.*)
                                                 (sql/from [:users :u])
                                                 (sql/join [:api_tokens :t] [:= :u.id :t.user_id])
                                                 (sql/where [:= :u.id user-id])
                                                 sql-format))))]
      (response {:user user-res
                 :token token-res
                 :auth auth-res}))
    (catch Exception e
      (error "Failed to get user" e)
      (bad-request {:error "Failed to get user" :details (.getMessage e)}))))

(defn get-user-profile [request]
  (try
    (let [tx (:tx request)
          user-id (or (presence (-> request :path-params :user_id))
                      (:id (:authenticated-entity request)))
          auth (convert-to-map (:authenticated-entity request))
          current (get-current tx auth)
          settings (get-settings tx user-id)
          navigation (get-navigation tx user-id)
          user-details (get-one tx (:target-user-id request) user-id)]
      (response {:user-id user-id
                 :current current
                 :settings settings
                 :navigation navigation
                 :user-details user-details
                 :language {:one-to-use (l/one-to-use tx user-id)
                            :get-one (l/get-one tx auth)
                            :get-multiple (l/get-multiple tx)}}))
    (catch Exception e
      (error "Failed to get user" e)
      (bad-request {:error "Failed to get user" :details (.getMessage e)}))))
