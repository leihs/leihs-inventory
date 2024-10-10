(ns leihs.inventory.server.resources.user.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [path-params]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]]))

(defn get-pools-of-user-handler [request]
  (try
    (let [tx (:tx request)
          user_id (-> request path-params :user_id)
          query (-> (sql/select :p.id :p.name :p.description)
                    (sql/from [:direct_access_rights :d])
                    (sql/join [:users :u] [:= :u.id :d.user_id])
                    (sql/join [:inventory_pools :p] [:= :d.inventory_pool_id :p.id])
                    (sql/where [:= :u.id user_id])
                    sql-format)
          result (jdbc/query tx query)]
      (response result))
    (catch Exception e
      (error "Failed to get user" e)
      (bad-request {:error "Failed to get user" :details (.getMessage e)}))))

(defn get-user-details-handler [request]
  (try
    (let [tx (:tx request)
          user_id (-> request path-params :user_id)

          user-query (-> (sql/select :u.id :u.login :u.email :u.firstname :u.lastname :u.organization :u.is_admin :u.org_id)
                         (sql/from [:users :u])
                         (sql/where [:= :u.id user_id])
                         sql-format)
          user-res (jdbc/query tx user-query)

          auth-query (-> (sql/select :a.id :a.authentication_system_id :a.created_at :a.updated_at)
                         (sql/from [:users :u])
                         (sql/join [:authentication_systems_users :a] [:= :u.id :a.user_id])
                         (sql/where [:= :u.id user_id])
                         sql-format)
          auth-res (jdbc/query tx auth-query)

          token-query (-> (sql/select :t.*)
                          (sql/from [:users :u])
                          (sql/join [:api_tokens :t] [:= :u.id :t.user_id])
                          (sql/where [:= :u.id user_id])
                          sql-format)
          token-result (jdbc/query tx token-query)
          token-res (vec (map #(dissoc % :token_hash :user_id :description) token-result))]

      (response {:user user-res
                 :token token-res
                 :auth auth-res}))
    (catch Exception e
      (error "Failed to get user" e)
      (bad-request {:error "Failed to get user" :details (.getMessage e)}))))
