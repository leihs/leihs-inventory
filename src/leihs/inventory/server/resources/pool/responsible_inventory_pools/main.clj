(ns leihs.inventory.server.resources.pool.responsible-inventory-pools.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [query-params]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [debug error spy]]))

;(defn get-pools-handler [request]
;  (let [tx (:tx request)
;        user-id (-> request :parameters :query :login)
;        _ (when (nil? user-id)
;            (throw (ex-info "Bad Request" {:status 400})))
;
;        select (sql/select :ip.*)
;        models-query (sql/union-all
;          ;; Query for direct_access_rights
;                      (-> select
;                          (sql/from [:users :u])
;                          (sql/join [:groups_users :gu] [:= :u.id :gu.user_id])
;                          (sql/join [:groups :g] [:= :gu.group_id :g.id])
;                          (sql/join [:direct_access_rights :dar] [:= :u.id :dar.user_id])
;                          (sql/join [:inventory_pools :ip] [:= :dar.inventory_pool_id :ip.id])
;                          (sql/where [:and
;                                      [:= :u.login user-id]
;                                      [:= :ip.is_active true]]))
;
;          ;; Query for group_access_rights
;                      (-> select
;                          (sql/from [:users :u])
;                          (sql/join [:groups_users :gu] [:= :u.id :gu.user_id])
;                          (sql/join [:groups :g] [:= :gu.group_id :g.id])
;                          (sql/join [:group_access_rights :gar] [:= :g.id :gar.group_id])
;                          (sql/join [:inventory_pools :ip] [:= :gar.inventory_pool_id :ip.id])
;                          (sql/where [:and
;                                      [:= :u.login user-id]
;                                      [:= :ip.is_active true]])))
;
;        res (jdbc/query tx (sql-format models-query))]
;    (response res)))

(defn get-responsible-pools-handler [request]
  (let [tx (:tx request)
        pool-id (-> request :parameters :path :pool_id)
        res (-> (sql/select :inventory_pools.id,
                            :inventory_pools.name,
                            :inventory_pools.shortname)
                (sql/from :inventory_pools)
                (sql/where [:exists
                            (-> (sql/select 1)
                                (sql/from :items)
                                (sql/where [:= :items.inventory_pool_id :inventory_pools.id])
                                (sql/where [:= :items.owner_id pool-id]))])
                (sql/order-by :inventory_pools.name)
                sql-format
                (->> (jdbc/query tx)))]
    (response res)))
