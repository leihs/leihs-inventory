(ns leihs.inventory.server.resources.dev.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [leihs.inventory.server.resources.utils.request :refer [query-params]]
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
          query-params (query-params request)
          type (:type query-params)

          p (println ">o> type" type)

          type (if (nil? type) "min" type)


          is-admin (if   (= type "all") [true false] [true])
          ;is-system-admin (if   (= type "all") [true false] [true])
          is-system-admin [true false]
          roles ["lending_manager" "inventory_manager" "group_manager" "customer"]
          pw "$2a$06$1bdwAZln616rr0WaJ4NisOa/YsXykCyi6Zs2q5ZgDW3.ZcfhkSmiy"

          ;; Helper function to build the base query
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
                                               [:= :u.is_system_admin is-system-admin]
                                               [:= :ua.role role]]
                                       :group-by [:ua.user_id :ua.inventory_pool_id :ip.name :u.is_admin :ua.role :u.login :u.email]}]]})

          ;; Helper function to build a specific query
          build-specific-query (fn [base-query type where-clause]
                                 (merge base-query
                                   {:select [:is_admin
                                             :user_id
                                             :login
                                             :email
                                             :inventory_pool_id
                                             :role
                                             ;:directa
                                             ;:groupa
                                             [[:inline type] :type]]
                                    :from [:user_access_summary]
                                    :where where-clause
                                    :limit 1}))

          ;; Execute the three specific queries for a given combination
          execute-queries (fn [is-system-admin is-admin role]
                            (let [base-query (build-base-query is-system-admin is-admin role)
                                  direct-query (build-specific-query base-query "direct_only" [:and [:= :directa 0] [:> :groupa 0]])
                                  group-query (build-specific-query base-query "group_only" [:and [:> :directa 0] [:= :groupa 0]])
                                  both-query (build-specific-query base-query "both" [:and [:> :directa 0] [:> :groupa 0]])]
                              {:direct (jdbc/execute! tx (sql-format direct-query))
                               :group (jdbc/execute! tx (sql-format group-query))
                               :both (jdbc/execute! tx (sql-format both-query))}))

          ;; Iterate over all combinations of is-admin, is-system-admin, and roles
          query-results (for [sadmin is-system-admin
                              admin is-admin
                              role roles]
                          (let [result (execute-queries sadmin admin role)
                                user-ids (->> result
                                           vals ; Get all query results
                                           (mapcat identity) ; Flatten the individual query results
                                           (map :user_id))] ; Extract user IDs
                            {:query {:is-system-admin sadmin :is-admin admin :role role}
                             :user-ids user-ids
                             :result result}))

          ;; Collect all distinct user IDs from the results
          all-user-ids (->> query-results
                         (mapcat :user-ids) ; Flatten all user-ids from the results
                         distinct
                         vec) ; Ensure all-user-ids is a vector

          ;; Perform the update operation
          update-result (when (seq all-user-ids) ; Only execute if user IDs exist
                          (jdbc/execute!
                            tx
                            (-> (sql/update :authentication_systems_users)
                              (sql/set {:data pw})
                              (sql/where [:and
                                          [:in :user_id all-user-ids]
                                          [:= :authentication_system_id "password"]])
                              sql-format)

                            ))
          ]

      ;; Return the final result with all unique user IDs and update confirmation
      (response {:result query-results
                 :all-user-ids all-user-ids
                 :update-result update-result}))

    (catch Exception e
      (error "Failed to get items" e)
      (bad-request {:error "Failed to get items"
                    :details (.getMessage e)}))))

