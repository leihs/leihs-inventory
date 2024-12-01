(ns leihs.inventory.server.resources.dev.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [honey.sql :as sq]
   ;[next.jdbc.sql :as jdbc]
   [next.jdbc :as jdbc]
   [leihs.inventory.server.resources.utils.request :refer [path-params]]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]]))


(defn update-and-fetch-accounts [request]
  (try
    (let [tx (:tx request)
          roles ["lending_manager" "inventory_manager" "group_manager" "customer"]
          is-admin [true false]
          is-system-admin [true false]

          ;; Helper function to build the query
          build-query (fn [is-system-admin is-admin role]
                        (-> (sql/select :u.is_system_admin :u.is_admin :uar.inventory_pool_id :uar.role :uar.user_id)
                          (sql/from [:unified_access_rights :uar])
                          (sql/join [:users :u] [:= :u.id :uar.user_id])
                          (sql/where [:and [:= :u.is_admin is-admin]
                                      [:= :u.is_system_admin is-system-admin]
                                      [:= :uar.role role]])
                          (sql/limit 2)
                          sql-format))

          ;; Iterate over all combinations of is-admin, is-system-admin, and roles
          query-results (for [sadmin is-system-admin
                              admin is-admin
                              role roles]
                          (let [query (build-query sadmin admin role)
                                _ (println ">o> query" query)
                                query-result (jdbc/execute! tx query)
                                user-ids (mapv :user_id query-result)]
                            {:query {:is-system-admin sadmin :is-admin admin :role role}
                             :user-ids user-ids
                             :result query-result}))

          ;; Collect all distinct user IDs from the results
          all-user-ids (->> query-results
                         (mapcat :user-ids) ; Flatten all user-ids from the results
                         distinct)] ; Remove duplicates

      ;; Return the final result with all unique user IDs
      (response {:result query-results :all-user-ids all-user-ids}))

    (catch Exception e
      (error "Failed to get items" e)
      (bad-request {:error "Failed to get items"
                    :details (.getMessage e)}))))
