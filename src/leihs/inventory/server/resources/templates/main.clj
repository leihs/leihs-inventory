(ns leihs.inventory.server.resources.templates.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [path-params]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response]]
   [taoensso.timbre :refer [error]]))

(defn get-templates-of-pool-handler [request]
  (try
    (let [tx (:tx request)
          pool_id (-> request path-params :pool_id)
          template_id (-> request path-params :template_id)
          query (-> (sql/select :mg.*)
                    (sql/from [:model_groups :mg])
                    (sql/where [:= :mg.type "Template"])
                    (cond-> template_id (sql/where [:= :mg.id template_id]))
                    (sql/order-by :mg.name)
                    sql-format)
          result (jdbc/query tx query)]
      (response result))
    (catch Exception e
      (error "Failed to get items" e)
      (bad-request {:error "Failed to get items" :details (.getMessage e)}))))
