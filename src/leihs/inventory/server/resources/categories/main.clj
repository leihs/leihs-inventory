(ns leihs.inventory.server.resources.categories.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [path-params]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]]))

(defn get-groups-of-pool-handler [request]
  (try
    (let [tx (:tx request)
          pool_id (-> request path-params :pool_id)
          group_id (-> request path-params :group_id)
          query (-> (sql/select :g.*)
                    (sql/from [:groups :g])
                    (sql/join [:group_access_rights :gar] [:= :g.id :gar.group_id])
                    (sql/where [:= :gar.inventory_pool_id pool_id])
                    (cond-> group_id (sql/where [:= :g.id group_id]))
                    (sql/limit 50)
                    sql-format)
          result (jdbc/query tx query)]
      (response result))
    (catch Exception e
      (error "Failed to get groups" e)
      (bad-request {:error "Failed to get groups" :details (.getMessage e)}))))

(defn get-entitlement-groups-of-pool-handler [request]
  (try
    (let [tx (:tx request)
          pool_id (-> request path-params :pool_id)
          group_id (-> request path-params :entitlement_group_id)
          query (-> (sql/select :g.*)
                    (sql/from [:entitlement_groups :g])
                    (sql/join [:inventory_pools :ip] [:= :g.inventory_pool_id :ip.id])
                    ;(sql/join [:group_access_rights :gar] [:= :g.id :gar.group_id])
                    ;(sql/where [:= :gar.inventory_pool_id pool_id])
                    (cond-> group_id (sql/where [:= :g.id group_id]))
                    (sql/limit 50)
                    sql-format)
          result (jdbc/query tx query)]
      (response result))
    (catch Exception e
      (error "Failed to get groups" e)
      (bad-request {:error "Failed to get groups" :details (.getMessage e)}))))

;; model_groups / model_group_links == category / category_links
(defn get-model-groups-of-pool-handler [request]
  (try
    (let [tx (:tx request)
          pool_id (-> request path-params :pool_id)
          group_id (-> request path-params :model_group_id)
          type "Category"
          query (-> (sql/select :mg.*)
                    (sql/from [:model_groups :mg]) ;; TODO: add hierarchy?
                    ;(sql/join [:inventory_pools_model_groups :ipmg] [:= :mg.id :ipmg.model_group_id])
                    ;(sql/join [:inventory_pools :ip] [:= :ipmg.inventory_pool_id :ip.id])
                    ;(sql/where [:= :ip.id pool_id])
                    ;(sql/where [:= :mg.type type])    ;;TODO: Category | Template
                    (sql/where [:ilike :mg.type (str type)])
                    (sql/order-by :mg.name)

                    (cond-> group_id (sql/where [:= :mg.id group_id]))
                    ;(sql/limit 50)
                    sql-format)
          result (jdbc/query tx query)]
      (response result))
    (catch Exception e
      (error "Failed to get items" e)
      (bad-request {:error "Failed to get items" :details (.getMessage e)}))))

;; TODO: hierarchies resolution
(defn get-model-group-links-of-pool-handler [request]
  (try
    (let [tx (:tx request)
          pool_id (-> request path-params :pool_id)
          group_id (-> request path-params :category_link_id)
          query (-> (sql/select :mgl.*)
                    (sql/from [:model_group_links :mgl])
                    ;(sql/join [:inventory_pools_model_groups :ipmg] [:= :mgl.id :ipmg.model_group_id])
                    ;(sql/join [:inventory_pools :ip] [:= :ipmg.inventory_pool_id :ip.id])
                    ;(sql/where [:= :ip.id pool_id])
                    (cond-> group_id (sql/where [:= :mgl.id group_id]))
                    (sql/limit 50)
                    sql-format)
          result (jdbc/query tx query)]
      (response result))
    (catch Exception e
      (error "Failed to get items" e)
      (bad-request {:error "Failed to get items" :details (.getMessage e)}))))
