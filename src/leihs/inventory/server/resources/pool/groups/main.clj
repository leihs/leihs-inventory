(ns leihs.inventory.server.resources.pool.groups.main
  (:require
   [clojure.set]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.utils.debug :refer [log-by-severity]]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [leihs.inventory.server.utils.request-utils :refer [query-params]]
   [ring.middleware.accept]
   [ring.util.response :refer [response]]))

(def ERROR_GET "Failed to get groups")

(defn index-resources [request]
  (try
    (let [{:keys [search]} (-> request query-params)
          base-query (-> (sql/select :g.id
                                     :g.name
                                     :g.searchable
                                     [[:count :gu.user_id] :user_count])
                         (sql/from [:groups :g])
                         (sql/join [:groups_users :gu] [:= :g.id :gu.group_id])
                         (sql/group-by :g.id :g.name :g.searchable)
                         (sql/order-by :g.name)
                         (cond-> search
                           (sql/where [:ilike :g.name (str "%" search "%")])))]
      (response (create-pagination-response request base-query nil)))
    (catch Exception e
      (log-by-severity ERROR_GET e)
      (exception-handler request ERROR_GET e))))
