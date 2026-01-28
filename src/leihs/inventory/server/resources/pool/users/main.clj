(ns leihs.inventory.server.resources.pool.users.main
  (:require
   [clojure.set]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.middlewares.debug :refer [log-by-severity]]
   [leihs.inventory.server.middlewares.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [leihs.inventory.server.utils.request :refer [query-params]]
   [ring.middleware.accept]
   [ring.util.response :refer [response]]))

(def ERROR_GET "Failed to get users")

(defn index-resources [request]
  (try
    (let [{:keys [search account_enabled]} (-> request query-params)
          base-query (-> (sql/select :u.id
                                     :u.firstname
                                     :u.lastname
                                     :u.login
                                     :u.email
                                     :u.searchable
                                     :u.account_enabled)
                         (sql/from [:users :u])
                         (cond-> search
                           (sql/where [:ilike :u.searchable (str "%" search "%")]))
                         (cond-> (some? account_enabled)
                           (sql/where [:= :u.account_enabled account_enabled]))
                         (sql/order-by :u.lastname :u.firstname :u.id))]
      (response (create-pagination-response request base-query nil)))
    (catch Exception e
      (log-by-severity ERROR_GET e)
      (exception-handler request ERROR_GET e))))
