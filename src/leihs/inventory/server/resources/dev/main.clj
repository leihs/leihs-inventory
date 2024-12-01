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

          ;; Helper function to generate query for a given is-admin and role
          ;build-query (fn [is-admin role]
          ;              (-> (sql/select :*)
          ;                (sql/from [:unified_access_rights :uar])
          ;                (sql/join [:users :u] [:u.id :uar.user_id])
          ;                (sql/where [:and [:= :u.is_admin is-admin]
          ;                          [:= :uar.role role]])
          ;                (sql/limit 1)
          ;                sql-format))

          build-query  (fn [is-system-admin is-admin role]
              ;(-> (sql/select :*)
              (-> (sql/select :u.is-system-admin :u.is-admin :uar.inventory_pool_id :uar.role)
                (sql/from [:unified_access_rights :uar])
                (sql/join [:users :u] [:= :u.id :uar.user_id])
                (sql/where [:and [:= :u.is_admin is-admin]
                            [:= :u.is_system_admin is-system-admin]
                          [:= :uar.role role]])
                (sql/limit 1)
                sql-format))


          ;; Iterate over all combinations of is-admin and roles
          result (for [sadmin is-system-admin
                       admin is-admin
                       role roles]
                   (let [query (build-query sadmin admin role)
                         p (println ">o> query" query)
                         query-result (jdbc/execute! tx query)
                         p (println ">o> query-result" query-result)
                         ]
                     {:query {:is-system-admin sadmin :is-admin admin :role role}
                      :result query-result}))

          p (println ">o> result" result)
          ]

      ;; Return the final result
      (response result))

    (catch Exception e
      (error "Failed to get items" e)
      (bad-request {:error "Failed to get items"
                    :details (.getMessage e)}))))

