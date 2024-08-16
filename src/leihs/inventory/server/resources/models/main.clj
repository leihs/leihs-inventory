(ns leihs.inventory.server.resources.models.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]])
  (:import (java.time LocalDateTime)))




(defn query-inventory-groups-by-login [request]
  ;[{:keys [login firstname lastname]}]
  (let [

        ;pool_id (get-in request [:path-params :pool_id])
        login (-> request :parameters :query :login)
        firstname (-> request :parameters :query :firstname)
        lastname (-> request :parameters :query :lastname)

        p (println ">o> login=" login (type login))
        p (println ">o> firstname=" firstname (type firstname))
        p (println ">o> lastname=" lastname (type lastname))


        ;session (-> request :session)
        ;auth-entity (-> request :authenticated-entity)
        ;p (println ">o> session=" session)
        ;p (println ">o> auth-entity=" auth-entity)




        tx (:tx request)


        ;result nil
        result (if (and (nil? login) (nil? firstname) (nil? lastname))
        ;result (if (and (nil? session) (nil? auth-entity) )
                 {:status 400 :body {:message "Please enter a filter value"}}
                 (let [
                       ;; Build the base query
                       base-query
                       (-> (sql/select-distinct :g.id :g.name :g.org_id :g.organization :gar.role :ip.id :ip.name :u.login)
                           (sql/from [:groups :g])
                           (sql/join [:groups_users :gu] [:= :g.id :gu.group_id])
                           (sql/join [:users :u] [:= :gu.user_id :u.id])
                           (sql/join [:group_access_rights :gar] [:= :g.id :gar.group_id])
                           (sql/join [:inventory_pools :ip] [:= :ip.id :gar.inventory_pool_id])


                           ;(sql/where [:= :u.id (:id auth-entity)])

                           ;; Add where conditions for login, firstname, and lastname
                           (cond-> (not (nil? login))
                             (sql/where [:ilike :u.login (str "%" login "%")]))
                           (cond-> (not (nil? firstname))
                             (sql/where [:ilike :u.firstname (str "%" firstname "%")]))
                           (cond-> (not (nil? lastname))
                             (sql/where [:ilike :u.lastname (str "%" lastname "%")]))

                           )

                       ;; Format the query
                       sql (sql-format base-query)
                       result (jdbc/query tx sql)

                       ] {:body result})
                 )

        ]
    result))


(defn query-inventory-groups [request]
  ;[{:keys [login firstname lastname]}]
  (let [

        ;;pool_id (get-in request [:path-params :pool_id])
        ;login (-> request :parameters :query :login)
        ;firstname (-> request :parameters :query :firstname)
        ;lastname (-> request :parameters :query :lastname)
        ;
        ;p (println ">o> login=" login (type login))
        ;p (println ">o> firstname=" firstname (type firstname))
        ;p (println ">o> lastname=" lastname (type lastname))


        session (-> request :session)
        auth-entity (-> request :authenticated-entity)
        p (println ">o> session=" session)
        p (println ">o> auth-entity=" auth-entity)




        tx (:tx request)


        ;result nil
        ;result (if (and (nil? login) (nil? firstname) (nil? lastname))
        result (if (and (nil? session) (nil? auth-entity) )
          {:status 400 :body {:message "Please enter a filter value"}}
          (let [
                ;; Build the base query
                base-query
                (-> (sql/select-distinct :g.id :g.name :g.org_id :g.organization :gar.role :ip.id :ip.name :u.login)
                    (sql/from [:groups :g])
                    (sql/join [:groups_users :gu] [:= :g.id :gu.group_id])
                    (sql/join [:users :u] [:= :gu.user_id :u.id])
                    (sql/join [:group_access_rights :gar] [:= :g.id :gar.group_id])
                    (sql/join [:inventory_pools :ip] [:= :ip.id :gar.inventory_pool_id])


                    (sql/where [:= :u.id (:id auth-entity)])

                    ;;; Add where conditions for login, firstname, and lastname
                    ;(cond-> (not (nil? login))
                    ;  (sql/where [:ilike :u.login (str "%" login "%")]))
                    ;(cond-> (not (nil? firstname))
                    ;  (sql/where [:ilike :u.firstname (str "%" firstname "%")]))
                    ;(cond-> (not (nil? lastname))
                    ;  (sql/where [:ilike :u.lastname (str "%" lastname "%")]))

                     )

                ;; Format the query
                sql (sql-format base-query)
                result (jdbc/query tx sql)

                ] {:body result})
          )

        ]
    result))



(defn query-user-handler-by-login [request]

  (println ">o> query-user-handler")

  ;[{:keys [login firstname lastname]}]
  (let [

        pool_id (get-in request [:path-params :pool_id])
        login (-> request :parameters :query :login)
        firstname (-> request :parameters :query :firstname)
        lastname (-> request :parameters :query :lastname)

        p (println ">o> login=" login (type login))
        p (println ">o> firstname=" firstname (type firstname))
        p (println ">o> lastname=" lastname (type lastname))

        ;session (-> request :session)
        ;auth-entity (-> request :authenticated-entity)
        ;p (println ">o> session=" session)
        ;p (println ">o> auth-entity=" auth-entity)



        tx (:tx request)

        result nil

        result (if (and (nil? login) (nil? firstname) (nil? lastname))
        ;result (if (and (nil? session) (nil? auth-entity))
                 {:status 400 :body {:message "Please enter a filter value"}}
                 (let [

                       base-query (-> (sql/select :*)
                                      (sql/from [:users :u])
                                      ;(sql/join [:api_tokens :api] [:= :u.id :api.user_id])
                                      ;(sql/where [:= :u.id (:id auth-entity)])

                                          ;; Add where conditions for login, firstname, and lastname
                                          (cond-> (not (nil? login))
                                            (sql/where [:ilike :u.login (str "%" login "%")]))
                                          (cond-> (not (nil? firstname))
                                            (sql/where [:ilike :u.firstname (str "%" firstname "%")]))
                                          (cond-> (not (nil? lastname))
                                            (sql/where [:ilike :u.lastname (str "%" lastname "%")]))
                                      )

                       ;; Build the base query
                       ;base-query
                       ;(-> (sql/select :u.login :u.id [:api.token_hash :token_hash] [:api.token_part :token_part] [:api.expires_at :expires_at])
                       ;    (sql/from [:users :u])
                       ;    (sql/join [:api_tokens :api] [:= :u.id :api.user_id])
                       ;
                       ;    (sql/where [:= :u.id (:id auth-entity)])
                       ;    ;;; Add where conditions for login, firstname, and lastname
                       ;    ;(cond-> (not (nil? login))
                       ;    ;  (sql/where [:ilike :u.login (str "%" login "%")]))
                       ;    ;(cond-> (not (nil? firstname))
                       ;    ;  (sql/where [:ilike :u.firstname (str "%" firstname "%")]))
                       ;    ;(cond-> (not (nil? lastname))
                       ;    ;  (sql/where [:ilike :u.lastname (str "%" lastname "%")]))
                       ;     )

                       ;; Format the query
                       sql (sql-format base-query)
                       result (jdbc/query tx sql)

                       ;result nil

                       ] {:body result})
                 )

        ]
    result))

(defn query-user-handler [request]

  (println ">o> query-user-handler")

  ;[{:keys [login firstname lastname]}]
  (let [

        ;pool_id (get-in request [:path-params :pool_id])
        ;login (-> request :parameters :query :login)
        ;firstname (-> request :parameters :query :firstname)
        ;lastname (-> request :parameters :query :lastname)
        ;
        ;p (println ">o> login=" login (type login))
        ;p (println ">o> firstname=" firstname (type firstname))
        ;p (println ">o> lastname=" lastname (type lastname))

        session (-> request :session)
        auth-entity (-> request :authenticated-entity)
        p (println ">o> session=" session)
        p (println ">o> auth-entity=" auth-entity)



        tx (:tx request)

        result nil

        ;result (if (and (nil? login) (nil? firstname) (nil? lastname))
        result (if (and (nil? session) (nil? auth-entity))
          {:status 400 :body {:message "Please enter a filter value"}}
          (let [

                base-query (-> (sql/select :*)
                             (sql/from [:users :u])
                             ;(sql/join [:api_tokens :api] [:= :u.id :api.user_id])
                             (sql/where [:= :u.id (:id auth-entity)]))

                ;;; Build the base query
                ;base-query
                ;(-> (sql/select :u.login :u.id [:api.token_hash :token_hash] [:api.token_part :token_part] [:api.expires_at :expires_at])
                ;    (sql/from [:users :u])
                ;    (sql/join [:api_tokens :api] [:= :u.id :api.user_id])
                ;
                ;    (sql/where [:= :u.id (:id auth-entity)])
                ;    ;;; Add where conditions for login, firstname, and lastname
                ;    ;(cond-> (not (nil? login))
                ;    ;  (sql/where [:ilike :u.login (str "%" login "%")]))
                ;    ;(cond-> (not (nil? firstname))
                ;    ;  (sql/where [:ilike :u.firstname (str "%" firstname "%")]))
                ;    ;(cond-> (not (nil? lastname))
                ;    ;  (sql/where [:ilike :u.lastname (str "%" lastname "%")]))
                ;     )

                ;; Format the query
                sql (sql-format base-query)
                result (jdbc/query tx sql)

                ;result nil

                ] {:body result})
          )

        ]
    result))

(defn get-models-of-pool-handler [request]
  (let [
        pool_id (get-in request [:path-params :pool_id])
        model_id (or (get-in request [:path-params :model_id]) (-> request :parameters :query :model_id))

        p (println ">o> pool_id=" pool_id (type pool_id))
        p (println ">o> model_id=" model_id (type model_id))

        tx (:tx request)

        recursive-cte {:with-recursive
                       [[[:model_group_hierarchy]
                         (-> (sql/select :mg.id [:model_group_id])
                             (sql/from [:model_groups :mg])
                             (sql/join [:inventory_pools_model_groups :ipmg]
                               [:= :mg.id :ipmg.model_group_id])
                             (sql/where [:= :ipmg.inventory_pool_id [:cast pool_id :uuid]]))
                         (-> (sql/select :mgl.child_id [:model_group_id])
                             (sql/from [:model_group_links :mgl])
                             (sql/join [:model_group_hierarchy :mgh]
                               [:= :mgl.parent_id :mgh.model_group_id]))]]}

        ;; Base query to select from the models
        base-query (-> (sql/select :m.id
                         :m.type
                         :m.manufacturer
                         :m.product
                         :m.version
                         :m.info_url
                         :m.rental_price
                         :m.maintenance_period
                         :m.is_package
                         :m.hand_over_note
                         :m.description
                         :m.internal_description
                         :m.technical_detail
                         :m.created_at
                         :m.updated_at)
                       (sql/from [:models :m])
                       (sql/join [:model_links :ml] [:= :m.id :ml.model_id])
                       (sql/join [:model_group_hierarchy :mgh] [:= :ml.model_group_id :mgh.model_group_id])
                       (cond-> (not (nil? model_id))
                         (sql/where [:= :m.id [:cast model_id :uuid]]))
                       ;(sql/order-by (or sort-by :m.id))
                       )

        sql (sql-format (merge recursive-cte base-query))
        result (jdbc/query tx sql)
        ]
    {:body result}))


(defn get-model_groups-of-pool-handler [request]
  (let [tx (:tx request)
        pool_id (get-in request [:path-params :pool_id])
        mg_id (or (get-in request [:path-params :model_group_id]) (-> request :parameters :query :model_group_id))

        recursive-cte {:with-recursive
                       [[[:model_group_hierarchy]
                         ;; Anchor part of the CTE
                         (-> (sql/select :mg.id :mg.name :mg.type
                               :mgl.parent_id :mgl.child_id
                               [[:raw "1"] :level])
                             (sql/from [:model_groups :mg])
                             (sql/join [:inventory_pools_model_groups :ipmg]
                               [:= :mg.id :ipmg.model_group_id])
                             (sql/left-join [:model_group_links :mgl]
                               [:= :mg.id :mgl.parent_id])
                             (sql/where [:= :ipmg.inventory_pool_id [:cast pool_id :uuid]]))

                         ;; Recursive part of the CTE
                         (-> (sql/select :mg.id :mg.name :mg.type
                               :mgl.parent_id :mgl.child_id
                               [[:raw "model_group_hierarchy.level + 1"] :level])
                             (sql/from [:model_group_links :mgl])
                             (sql/join [:model_groups :mg] [:= :mgl.child_id :mg.id])
                             (sql/join [:model_group_hierarchy]
                               [:= :mgl.parent_id :model_group_hierarchy.id]))]]}

        ;; Final select query after the CTE
        base-query (-> (sql/select-distinct :id :name :type)
                       (sql/from [:model_group_hierarchy])
                       (sql/order-by :id)
                       (cond-> (not (nil? mg_id))
                         (sql/where [:= :id [:cast mg_id :uuid]]))
                       )

        ;; Combine the recursive CTE and the base query
        sql (sql-format (merge recursive-cte base-query))
        result (jdbc/query tx sql)
        ]
    {:body result}))


(defn get-pools-handler [request]
  (let [tx (:tx request)
        sql (sql-format
              (-> (sql/select :ip.id :ip.name)
                  (sql/from [:inventory_pools :ip])
                  (sql/order-by :ip.name)))
        result (jdbc/query tx sql)]
    {:body result}))

(defn get-models-handler
  [{:keys [inventory-pool-id filter-manufacturer filter-product sort-by]}]
  ;; Base CTE for recursive model group hierarchy
  (let [with-clause
        "WITH RECURSIVE model_group_hierarchy AS (
            SELECT mg.id AS model_group_id
            FROM model_groups mg
            JOIN inventory_pools_model_groups ipmg
              ON mg.id = ipmg.model_group_id
            WHERE ipmg.inventory_pool_id = ?

            UNION ALL

            SELECT mgl.child_id AS model_group_id
            FROM model_group_links mgl
            JOIN model_group_hierarchy mgh
              ON mgl.parent_id = mgh.model_group_id
          )"

        ;; Base HoneySQL query for selecting models
        base-query
        (-> (sql/select :m.id
              :m.type
              :m.manufacturer
              :m.product
              :m.version
              :m.info_url
              :m.rental_price
              :m.maintenance_period
              :m.is_package
              :m.hand_over_note
              :m.description
              :m.internal_description
              :m.technical_detail
              :m.created_at
              :m.updated_at)
            (sql/from [:models :m])
            (sql/join [:model_links :ml] [:= :m.id :ml.model_id])
            (sql/join [:model_group_hierarchy :mgh] [:= :ml.model_group_id :mgh.model_group_id])

            ;; Apply manufacturer filter if provided
            (cond-> filter-manufacturer
              (sql/where [:ilike :m.manufacturer (str "%" filter-manufacturer "%")]))

            ;; Apply product filter if provided
            (cond-> filter-product
              (sql/where [:ilike :m.product (str "%" filter-product "%")]))

            ;; Apply sorting if provided
            (sql/order-by (or sort-by :m.id)))]

    ;; Combine the CTE and the base query
    (sql-format {:with-raw [with-clause]                    ;; CTE raw SQL
                 :select base-query
                 :params [inventory-pool-id]})))            ;; Pass inventory_pool_id as a parameter

(defn create-model-handler [request]
  (let [created_ts (LocalDateTime/now)
        body-params (:body-params request)
        tx (:tx request)
        model body-params
        model (assoc body-params
                     :created_at created_ts
                     :updated_at created_ts)]

    (try
      (let [res (jdbc/insert! tx :models model)]
        (if res
          (response res)
          (bad-request {:error "Failed to create model"})))

      (catch Exception e
        (error "Failed to create model" e)
        (bad-request {:error "Failed to create model" :details (.getMessage e)})))))

(defn update-model-handler [request]
  (let [model-id (get-in request [:path-params :id])
        body-params (:body-params request)
        tx (:tx request)
        model body-params]

    (try
      (let [res (jdbc/update! tx :models model ["id = ?::uuid" model-id])]

        (if (= 1 (:next.jdbc/update-count res))
          (response {:message "Model updated" :id model-id})
          (bad-request {:error "Failed to update model" :details "Model not found"})))

      (catch Exception e
        (error "Failed to update model" e)
        (bad-request {:error "Failed to update model" :details (.getMessage e)})))))

(defn delete-model-handler [request]
  (let [tx (:tx request)
        model-id (get-in request [:path-params :id])]

    (try
      (let [res (jdbc/delete! tx :models ["id = ?::uuid" model-id])]

        (if (= 1 (:next.jdbc/update-count res))
          (response {:message "Model deleted" :id model-id})
          (bad-request {:error "Failed to delete model" :details "Model not found"})))

      (catch Exception e
        (error "Failed to delete model" e)
        (status (bad-request {:error "Failed to delete model"
                              :details (.getMessage e)}) 409)))))
