(ns leihs.inventory.server.resources.pool.categories.category.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [path-params]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]]))




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

