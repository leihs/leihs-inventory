(ns leihs.inventory.server.resources.pools.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [query-params]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]]))

(defn get-pools-handler [request]
  (let [tx (:tx request)
        login (:login (query-params request))
        select (sql/select :ip.*)
        models-query (sql/union-all
          ;; Query for direct_access_rights
                      (-> select
                          (sql/from [:users :u])
                          (sql/join [:groups_users :gu] [:= :u.id :gu.user_id])
                          (sql/join [:groups :g] [:= :gu.group_id :g.id])
                          (sql/join [:direct_access_rights :dar] [:= :u.id :dar.user_id])
                          (sql/join [:inventory_pools :ip] [:= :dar.inventory_pool_id :ip.id])
                          (sql/where [:and
                                      [:= :u.login login]
                                      [:= :ip.is_active true]]))

          ;; Query for group_access_rights
                      (-> select
                          (sql/from [:users :u])
                          (sql/join [:groups_users :gu] [:= :u.id :gu.user_id])
                          (sql/join [:groups :g] [:= :gu.group_id :g.id])
                          (sql/join [:group_access_rights :gar] [:= :g.id :gar.group_id])
                          (sql/join [:inventory_pools :ip] [:= :gar.inventory_pool_id :ip.id])
                          (sql/where [:and
                                      [:= :u.login login]
                                      [:= :ip.is_active true]])))

        res (jdbc/query tx (sql-format models-query))]
    (response res)))