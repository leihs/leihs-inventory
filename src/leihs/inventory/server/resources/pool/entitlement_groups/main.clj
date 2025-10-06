(ns leihs.inventory.server.resources.pool.entitlement-groups.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.helper :refer [log-by-severity]]
   [leihs.inventory.server.utils.request-utils :refer [path-params]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [taoensso.timbre :refer [debug error]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [ring.util.response :refer [response ]]))

(def ERROR_GET "Failed to get entitlement-groups")

(defn index-resources [request]
  ;(try
    (let [tx (:tx request)
          pool_id (-> request path-params :pool_id)

          p (println ">o> abc.pool_id" pool_id)
          query (-> (sql/select :g.*)
                    (sql/from [:entitlement_groups :g])
                    (sql/join [:inventory_pools :ip] [:= :g.inventory_pool_id :ip.id])
                    (cond-> pool_id (sql/where [:= :g.inventory_pool_id pool_id]))
                    (sql/order-by :g.name)
                    (sql/limit 50)
                    ;sql-format
                     )
          ;result (jdbc/query tx query)]
      ;(response result))
    ;(catch Exception e
    ;  (log-by-severity ERROR_GET e)
    ;  (exception-handler request ERROR_GET e))))

post-fnc (fn [models]
           (->> models
             (fetch-thumbnails-for-ids tx)
             (map (model->enrich-with-image-attr pool-id))))

 ]


(println (sql-format query :inline true))
(response (create-pagination-response request query nil ))))
;(response (create-pagination-response request query nil post-fnc))))
