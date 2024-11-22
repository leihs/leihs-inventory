(ns leihs.inventory.server.resources.dev.main
  (:require
   [clojure.set]
   [honey.sql :as sq]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [path-params]]
   [leihs.inventory.server.resources.utils.request :refer [query-params]]
   [next.jdbc :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]]))

(defn update-and-fetch-accounts [request]
  (try
    (let [tx (:tx request)
          query-params (query-params request)
          type (:type query-params)
          type (if (nil? type) "min" type)
          is-admin (cond
                     (= type "all") [true false]
                     (= type "no") [false]
                     :else [false])
          is-system-admin (cond
                            (= type "all") [true false]
                            (= type "no") [false]
                            :else [false])

          roles ["lending_manager" "inventory_manager" "group_manager" "customer"]
          pw "$2a$06$1bdwAZln616rr0WaJ4NisOa/YsXykCyi6Zs2q5ZgDW3.ZcfhkSmiy"

          build-base-query (fn [is-system-admin is-admin role]
                             {:with [[[:user_access_summary]
                                      {:select [:u.is_admin
                                                :u.login
                                                :u.email
                                                :ua.user_id
                                                :ua.inventory_pool_id
                                                [:ip.name :pool_name]
                                                :ua.role
                                                [[:count :ua.direct_access_right_id] :directa]
                                                [[:count :ua.group_access_right_id] :groupa]]
                                       :from [[:unified_access_rights :ua]]
                                       :join [[:inventory_pools :ip] [:= :ip.id :ua.inventory_pool_id]
                                              [:users :u] [:= :u.id :ua.user_id]]
                                       :where [:and
                                               [:= :u.is_admin is-admin]
                                               [:is-not :u.login nil]
                                               [:= :u.is_system_admin is-system-admin]
                                               [:= :ua.role role]]
                                       :group-by [:ua.user_id :ua.inventory_pool_id :ip.name :u.is_admin :ua.role :u.login :u.email]}]]})

          build-specific-query (fn [base-query type where-clause]
                                 (merge base-query
                                        {:select [:is_admin
                                                  :user_id
                                                  :login
                                                  :email
                                                  :inventory_pool_id
                                                  :role
                                                  [[:inline type] :type]]
                                         :from [:user_access_summary]
                                         :where where-clause
                                         :limit 1}))

          execute-queries (fn [is-system-admin is-admin role]
                            (let [base-query (build-base-query is-system-admin is-admin role)
                                  direct-query (build-specific-query base-query "direct_only" [:and [:= :directa 0] [:> :groupa 0]])
                                  group-query (build-specific-query base-query "group_only" [:and [:> :directa 0] [:= :groupa 0]])
                                  both-query (build-specific-query base-query "both" [:and [:> :directa 0] [:> :groupa 0]])]
                              {:direct (jdbc/execute! tx (sql-format direct-query))
                               :group (jdbc/execute! tx (sql-format group-query))
                               :both (jdbc/execute! tx (sql-format both-query))}))

          query-results (for [sadmin is-system-admin
                              admin is-admin
                              role roles]
                          (let [result (execute-queries sadmin admin role)
                                user-ids (->> result
                                              vals
                                              (mapcat identity)
                                              (map :user_id))]
                            {:query {:is-system-admin sadmin :is-admin admin :role role}
                             :user-ids user-ids
                             :result result}))

          all-user-ids (->> query-results
                            (mapcat :user-ids)
                            distinct
                            vec)

          update-result (when (seq all-user-ids)
                          (jdbc/execute!
                           tx
                           (-> (sql/update :authentication_systems_users)
                               (sql/set {:data pw})
                               (sql/where [:and
                                           [:in :user_id all-user-ids]
                                           [:= :authentication_system_id "password"]])
                               sql-format)))]
      (response {:result query-results
                 :all-user-ids all-user-ids
                 :update-result update-result}))

    (catch Exception e
      (error "Failed to get items" e)
      (bad-request {:error "Failed to get items"
                    :details (.getMessage e)}))))
