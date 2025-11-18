(ns leihs.inventory.server.resources.pool.users.main
  (:require
   [clojure.set]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.utils.debug :refer [log-by-severity]]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [leihs.inventory.server.utils.request-utils :refer [query-params]]
   [ring.middleware.accept]
   [ring.util.response :refer [response]]))

(def ERROR_GET "Failed to get users")

(defn index-resources [request]
  (try
    (let [{:keys [search]} (-> request query-params)
          base-query (-> (sql/select :u.id
                                     :u.firstname
                                     :u.lastname
                                     :u.login
                                     :u.email
                                     :u.searchable)
                         (sql/from [:users :u])
                         (cond-> search
                           (sql/where [:ilike :u.searchable (str "%" search "%")])))]
      (response (create-pagination-response request base-query nil)))
    (catch Exception e
      (log-by-severity ERROR_GET e)
      (exception-handler request ERROR_GET e))))
