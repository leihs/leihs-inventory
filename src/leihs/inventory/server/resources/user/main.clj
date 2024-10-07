(ns leihs.inventory.server.resources.user.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [path-params]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   ;[honey.sql :refer [format format-expr] :rename {format sql-format}]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]]))

(defn get-pools-of-user-handler [request]
  (try (let [tx (:tx request)

             user_id (-> request path-params :user_id)
             p (println ">o> user_id" user_id (type user_id))

             query (-> (sql/select :p.id :p.name :p.description)
                     (sql/from [:direct_access_rights :d])
                     (sql/join [:users :u] [:= :u.id :d.user_id])
                     (sql/join [:inventory_pools :p] [:= :d.inventory_pool_id :p.id])
                     (sql/where [:= :u.id user_id])         ; Adjusted column name here for clarity
                     sql-format)

             p (println ">o> query" query)
             result (jdbc/query tx query)

             p (println ">o> result" result)
             ]

         (response result)
         )
       (catch Exception e
         (error "Failed to get pools of user" e)
         (bad-request {:error "Failed to get pools of user" :details (.getMessage e)}))
       )

  )


(defn get-user-details-handler [request]
  (try (let [tx (:tx request)

             auth-res nil
             token-res nil



             user_id (-> request path-params :user_id)
             p (println ">o> user_id" user_id (type user_id))

             ;query (-> (sql/select :u.*)
             query (-> (sql/select :u.id :u.login :u.email :u.firstname :u.lastname :u.organization :u.is_admin :u.org_id)
                     (sql/from [:users :u])
                     (sql/where [:= :u.id user_id])
                     sql-format)

             p (println ">o> query" query)
             result (jdbc/query tx query)
              user-res result

             p (println ">o> result" result)



             ;query (-> (sql/select :u.*)
             query (-> (sql/select :a.id :a.authentication_system_id :a.created_at :a.updated_at)
                     (sql/from [:users :u])
                     (sql/join [:authentication_systems_users :a] [:= :u.id :a.user_id])
                     (sql/where [:= :u.id user_id])
                     sql-format)

             p (println ">o> query" query)
             result (jdbc/query tx query)
             ;result (dissoc result [:created_at :updated_at])

             auth-res result

             p (println ">o> result" result)


             query (-> (sql/select :t.*)
             ;query (-> (sql/select :id :token_hash :token_part :account_enabled )
                     (sql/from [:users :u])
                     (sql/join [:api_tokens :t] [:= :u.id :t.user_id])
                     (sql/where [:= :u.id user_id])
                     sql-format)

             p (println ">o> query" query)
             result (jdbc/query tx query)
             p (println ">o> result" result)
             ;result (select-keys result [:id :token_hash :token_part :account_enabled])

             ;result (dissoc result [:token_hash :user_id :description])

             result (vec (map #(dissoc % :token_hash :user_id :description) result))


             token-res result

             p (println ">o> result.token" result)
             ]

         (response {:user user-res
                    :token token-res
                    :auth auth-res})
         )
       (catch Exception e
         (error "Failed to get pools of user" e)
         (bad-request {:error "Failed to get pools of user" :details (.getMessage e)}))
       )

  )
