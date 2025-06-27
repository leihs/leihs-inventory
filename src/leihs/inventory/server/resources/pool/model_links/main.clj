(ns leihs.inventory.server.resources.pool.model-links.main
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
          model_link_id (-> request path-params :id)
          query (-> (sql/select :m.id :m.quantity :mg.name :mg.type :ip.name :ip.shortname :ip.is_active)
                    (sql/from [:model_links :m])
                    (sql/join [:model_groups :mg] [:= :m.model_group_id :mg.id])
                    (sql/join [:inventory_pools_model_groups :ipmg] [:= :mg.id :ipmg.model_group_id])
                    (sql/join [:inventory_pools :ip] [:= :ip.id :ipmg.inventory_pool_id])
                    (sql/where [:= :ip.id pool_id])
                    (cond-> model_link_id (sql/where [:= :m.id model_link_id]))
                    (sql/limit 10)
                    sql-format)

          result (jdbc/query tx query)]
      (response result))
    (catch Exception e
      (error "Failed to get model-links" e)
      (bad-request {:error "Failed to get model-links" :details (.getMessage e)}))))
