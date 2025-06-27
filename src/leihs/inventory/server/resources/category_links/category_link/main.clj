(ns leihs.inventory.server.resources.category-links.category-link.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [path-params]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]]))


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