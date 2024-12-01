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

;(defn update-and-fetch-accounts [request]
;  (try
;    (let [tx (:tx request)
;          roles ["lending_manager" "inventory_manager" "group_manager" "customer"]
;          is-admin [true false]
;          is-system-admin [true false]
;
;          ;; Helper function to build the SQL query
;          build-query (fn [is-system-admin is-admin role]
;                        (sql-format
;                          {:with [[[:user_access_summary]
;                                   {:select [:u.is_admin
;                                             :ua.user_id
;                                             :ua.inventory_pool_id
;                                             [:ip.name :pool_name]
;                                             :ua.role
;                                             [[:count :ua.direct_access_right_id] :directa]
;                                             [[:count :ua.group_access_right_id] :groupa]]
;                                    :from [[:unified_access_rights :ua]]
;                                    :join [[:inventory_pools :ip] [:= :ip.id :ua.inventory_pool_id]
;                                           [:users :u] [:= :u.id :ua.user_id]]
;                                    :where [:and
;                                            [:= :u.is_admin is-admin]
;                                            [:= :u.is_system_admin is-system-admin]
;                                            [:= :ua.role role]]
;                                    :group-by [:ua.user_id :ua.inventory_pool_id :ip.name :u.is_admin :ua.role]}]]
;
;                           :union-all [(-> {:select [:*
;                                                     [[:inline "direct_only"] :type]]
;                                            :from [:user_access_summary]
;                                            :where [:and [:= :directa 0] [:> :groupa 0]]
;                                            :limit 1})
;                                       (-> {:select [:*
;                                                     [[:inline "group_only"] :type]]
;                                            :from [:user_access_summary]
;                                            :where [:and [:> :directa 0] [:= :groupa 0]]
;                                            :limit 1})
;                                       ;(-> {:select [:*
;                                       ;              [[:inline "both"] :type]]
;                                       ;     :from [:user_access_summary]
;                                       ;     :where [:and [:> :directa 0] [:> :groupa 0]]
;                                       ;     :limit 1})
;                                       ]}))
;
;          ;; Iterate over all combinations of is-admin, is-system-admin, and roles
;          query-results (for [sadmin is-system-admin
;                              admin is-admin
;                              role roles]
;                          (let [query (build-query sadmin admin role)
;                                _ (println ">o> query" query)
;                                query-result (jdbc/execute! tx query)
;                                user-ids (mapv :user_id query-result)]
;                            {:query {:is-system-admin sadmin :is-admin admin :role role}
;                             :user-ids user-ids
;                             :result query-result}))
;
;          ;; Collect all distinct user IDs from the results
;          all-user-ids (->> query-results
;                         (mapcat :user-ids) ; Flatten all user-ids from the results
;                         distinct)] ; Remove duplicates
;
;      ;; Return the final result with all unique user IDs
;      (response {:result query-results :all-user-ids all-user-ids}))
;
;    (catch Exception e
;      (error "Failed to get items" e)
;      (bad-request {:error "Failed to get items"
;                    :details (.getMessage e)}))))



(defn update-and-fetch-accounts [request]
  (try
    (let [tx (:tx request)
          roles ["lending_manager" "inventory_manager" "group_manager" "customer"]
          is-admin [true false]
          is-system-admin [true false]

          ;; Helper function to build the SQL query
          build-query (fn [is-system-admin is-admin role]
                        (sql-format
                          {:with [[[:user_access_summary]
                                   {:select [:u.is_admin
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
                                    :group-by [:ua.user_id :ua.inventory_pool_id :ip.name :u.is_admin :ua.role]}]]

                           ;; Each SELECT query for the UNION ALL
                           :union-all [{:select [:*
                                                 [[:inline "direct_only"] :type]]
                                        :from [:user_access_summary]
                                        :where [:and [:= :directa 0] [:> :groupa 0]]
                                        :limit 1}
                                       {:select [:*
                                                 [[:inline "group_only"] :type]]
                                        :from [:user_access_summary]
                                        :where [:and [:> :directa 0] [:= :groupa 0]]
                                        :limit 1}
                                       {:select [:*
                                                 [[:inline "both"] :type]]
                                        :from [:user_access_summary]
                                        :where [:and [:> :directa 0] [:> :groupa 0]]
                                        :limit 1}]}))

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


(defn update-and-fetch-accounts [request]
  (try
    (let [tx (:tx request)
          roles ["lending_manager" "inventory_manager" "group_manager" "customer"]
          is-admin [true false]
          is-system-admin [true false]

          ;; Updated helper function to build the SQL query
          build-query (fn [is-system-admin is-admin role]
                        (sql-format
                          {:with [[[:user_access_summary]
                                   {:select [:u.is_admin
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
                                    :group-by [:ua.user_id :ua.inventory_pool_id :ip.name :u.is_admin :ua.role]}]]

                           :select [:*]
                           :from [[{:select [:*
                                             [[:inline "direct_only"] :type]]
                                    :from [:user_access_summary]
                                    :where [:and [:= :directa 0] [:> :groupa 0]]
                                    :limit 1}]
                                  [{:select [:*
                                             [[:inline "group_only"] :type]]
                                    :from [:user_access_summary]
                                    :where [:and [:> :directa 0] [:= :groupa 0]]
                                    :limit 1}]
                                  [{:select [:*
                                             [[:inline "both"] :type]]
                                    :from [:user_access_summary]
                                    :where [:and [:> :directa 0] [:> :groupa 0]]
                                    :limit 1}]]
                           :union-all true}))

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



(defn update-and-fetch-accounts [request]
  (try
    (let [tx (:tx request)
          roles ["lending_manager" "inventory_manager" "group_manager" "customer"]
          is-admin [true false]
          is-system-admin [true false]

          ;; Helper function to build the SQL query
          build-query (fn [is-system-admin is-admin role]
                        (sql-format
                          {:with [[[:user_access_summary]
                                   {:select [:u.is_admin
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
                                            [:= :u.is_admin (boolean is-admin)]
                                            [:= :u.is_system_admin (boolean is-system-admin)]
                                            [:= :ua.role role]]
                                    :group-by [:ua.user_id :ua.inventory_pool_id :ip.name :u.is_admin :ua.role]}]]

                           ;; UNION ALL Queries
                           :union-all [{:select [:*
                                                 [[:inline "direct_only"] :type]]
                                        :from [:user_access_summary]
                                        :where [:and [:= :directa 0] [:> :groupa 0]]
                                        :limit 1}
                                       {:select [:*
                                                 [[:inline "group_only"] :type]]
                                        :from [:user_access_summary]
                                        :where [:and [:> :directa 0] [:= :groupa 0]]
                                        :limit 1}
                                       {:select [:*
                                                 [[:inline "both"] :type]]
                                        :from [:user_access_summary]
                                        :where [:and [:> :directa 0] [:> :groupa 0]]
                                        :limit 1}]}))

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

(defn update-and-fetch-accounts [request]
  (try
    (let [tx (:tx request)
          roles ["lending_manager" "inventory_manager" "group_manager" "customer"]
          is-admin [true false]
          is-system-admin [true false]

          ;; Helper function to build the SQL query
          build-query (fn [is-system-admin is-admin role]
                        (sql-format
                          {:with [[[:user_access_summary]
                                   {:select [:u.is_admin
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
                                    :group-by [:ua.user_id :ua.inventory_pool_id :ip.name :u.is_admin :ua.role]}]]

                           ;; UNION ALL Queries
                           :union-all [{:select [:u.is_admin
                                                 :ua.user_id
                                                 :ua.inventory_pool_id
                                                 :ua.role
                                                 :directa
                                                 :groupa
                                                 [[:inline "direct_only"] :type]]
                                        :from [:user_access_summary]
                                        :where [:and [:= :directa 0] [:> :groupa 0]]
                                        :limit 1}
                                       {:select [:u.is_admin
                                                 :ua.user_id
                                                 :ua.inventory_pool_id
                                                 :ua.role
                                                 :directa
                                                 :groupa
                                                 [[:inline "group_only"] :type]]
                                        :from [:user_access_summary]
                                        :where [:and [:> :directa 0] [:= :groupa 0]]
                                        :limit 1}
                                       {:select [:u.is_admin
                                                 :ua.user_id
                                                 :ua.inventory_pool_id
                                                 :ua.role
                                                 :directa
                                                 :groupa
                                                 [[:inline "both"] :type]]
                                        :from [:user_access_summary]
                                        :where [:and [:> :directa 0] [:> :groupa 0]]
                                        :limit 1}]}))

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


(defn update-and-fetch-accounts [request]
  (try
    (let [tx (:tx request)
          roles ["lending_manager" "inventory_manager" "group_manager" "customer"]
          is-admin [true false]
          is-system-admin [true false]

          ;; Helper function to build the base query
          build-base-query (fn [is-system-admin is-admin role]
                             {:with [[[:user_access_summary]
                                      {:select [:u.is_admin
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
                                       :group-by [:ua.user_id :ua.inventory_pool_id :ip.name :u.is_admin :ua.role]}]]})

          ;; Helper function to build specific queries
          build-specific-query (fn [base-query type where-clause]
                                 (merge base-query
                                   {:select [:u.is_admin
                                             :ua.user_id
                                             :ua.inventory_pool_id
                                             :ua.role
                                             :directa
                                             :groupa
                                             [[:inline type] :type]]
                                    :from [:user_access_summary]
                                    :where where-clause
                                    :limit 1}))

          ;; Execute the queries for each type
          execute-queries (fn [is-system-admin is-admin role]
                            (let [base-query (build-base-query is-system-admin is-admin role)
                                  direct-query (build-specific-query base-query "direct_only" [:and [:= :directa 0] [:> :groupa 0]])
                                  group-query (build-specific-query base-query "group_only" [:and [:> :directa 0] [:= :groupa 0]])
                                  both-query (build-specific-query base-query "both" [:and [:> :directa 0] [:> :groupa 0]])]
                              {:direct (jdbc/execute! tx (sql-format direct-query))
                               :group (jdbc/execute! tx (sql-format group-query))
                               :both (jdbc/execute! tx (sql-format both-query))}))

          ;; Iterate over all combinations and collect results
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
                         distinct)] ; Remove duplicates

      ;; Return the final result with all unique user IDs
      (response {:result query-results :all-user-ids all-user-ids}))

    (catch Exception e
      (error "Failed to get items" e)
      (bad-request {:error "Failed to get items"
                    :details (.getMessage e)}))))


(defn update-and-fetch-accounts [request]
  (try
    (let [tx (:tx request)
          roles ["lending_manager" "inventory_manager" "group_manager" "customer"]
          is-admin [true false]
          is-system-admin [true false]

          ;; Helper function to build the base query
          build-base-query (fn [is-system-admin is-admin role]
                             {:with [[[:user_access_summary]
                                      {:select [:u.is_admin
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
                                       :group-by [:ua.user_id :ua.inventory_pool_id :ip.name :u.is_admin :ua.role]}]]})

          ;; Helper function to build a specific query
          build-specific-query (fn [base-query type where-clause]
                                 (merge base-query
                                   {:select [:is_admin
                                             :user_id
                                             :inventory_pool_id
                                             :role
                                             :directa
                                             :groupa
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
                         distinct)] ; Remove duplicates

      ;; Return the final result with all unique user IDs
      (response {:result query-results :all-user-ids all-user-ids}))

    (catch Exception e
      (error "Failed to get items" e)
      (bad-request {:error "Failed to get items"
                    :details (.getMessage e)}))))
