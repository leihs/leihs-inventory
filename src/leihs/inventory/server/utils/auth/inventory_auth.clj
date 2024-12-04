(ns leihs.inventory.server.utils.auth.inventory-auth
  (:require
   [leihs.inventory.server.resources.auth.auth-routes :refer [extract-basic-auth-from-header verify-password-entry]]


   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   ;[leihs.inventory.server.resources.auth.session :as ab]
   ;[leihs.inventory.server.resources.utils.request :refer [AUTHENTICATED_ENTITY authenticated? get-auth-entity]]
   [next.jdbc :as jdbc]

   [ring.util.response :as response]))

(def CONST_LOG_AUTH true)

(defn log-auth-details [authenticated-entity]
  (when CONST_LOG_AUTH
    (let [user (:login authenticated-entity)
          email (:email authenticated-entity)
          auth-meth (:authentication-method authenticated-entity)]
      (println (str "> Authenticated: " user " / " email " authenticated with " auth-meth " / ")))))

(defn access-rights [tx user-id]
  (-> (sql/select :role :inventory_pool_id)
    (sql/from :access_rights)
    (sql/where [:= :user_id user-id])
    sql-format
    (->> (jdbc/execute-one! tx))))

(defn authenticated-user-entity [{tx :tx :as request}]

  (let [
        p (println ">o> authenticated-user-entity!!!!!!!!!!!!!")

        [login password] (extract-basic-auth-from-header request)
        _ (println ">o> login and password:" login password)
        user (verify-password-entry request login password)
        _ (println ">o> user:" user)


        ;p (println ">o> keys-session" (keys (:authenticated-entity request)))
        ;request-with-auth (assoc request :authenticated-entity user)




        request (if user
          (let [auth (assoc user
            :authentication-method :session
            :access-rights (access-rights tx (:id user))
            :scope_read true
            :scope_write true
            :scope_admin_read (:is_admin user)
            :scope_admin_write (:is_admin user)
            :scope_system_admin_read (:is_system_admin user)
            :scope_system_admin_write (:is_system_admin user))]

            (assoc request :authenticated-entity auth)

            )
          request
          )



        ]
request
    )
  )

(defn wrap-check-auth
  [handler check-admin?]
  (fn [request]
    (let [

          ;[login password] (extract-basic-auth-from-header request)
          ;_ (println ">o> login and password:" login password)
          ;user (verify-password-entry request login password)
          ;_ (println ">o> user:" user)
          ;
          ;
          ;p (println ">o> keys-session" (keys (:authenticated-entity request)))




          request-with-auth (assoc request :authenticated-entity (authenticated-user-entity request))


          authenticated-entity (if (nil? (:authenticated-entity request))
                              ;(assoc request :authenticated-entity (authenticated-user-entity request))
                              (:authenticated-entity (authenticated-user-entity request))
                              (:authenticated-entity request))


          p (println ">o> authenticated-entity" authenticated-entity)


          p (println ">o> abc1" (:is_admin authenticated-entity))
          p (println ">o> abc2" (get authenticated-entity :is_admin ))
          ;p (println ">o> abc2b" (get authenticated-entity [:is_admin] ))
          ;p (println ">o> abc3" (get-in authenticated-entity :is_admin ))
          p (println ">o> abc3b" (get-in authenticated-entity [:is_admin] ))

          ;authenticated-entity (:authenticated-entity request-with-auth)
          p (println ">o> keys-session" (keys (:authenticated-entity request)))
          ;p (println ">o> keys-session" (keys user))


          ]


      (println ">o> authenticated-entity:" authenticated-entity)

      (if (and authenticated-entity
            (or (not check-admin?)
              (:is_admin authenticated-entity)))
        (do
          (log-auth-details authenticated-entity)
          (handler request-with-auth))
        (response/status
          (response/response {:status "failure" :message "Access denied"}) 403)))))

(defn wrap-check-authenticated-admin [handler]
  (wrap-check-auth handler true))

(defn wrap-check-authenticated [handler]
  (wrap-check-auth handler false))



;(let [[login password] (extract-basic-auth-from-header request)
;      user (verify-password-entry request login password)]
;  (if user
;    (let [token (str (UUID/randomUUID))
;          hashed-token (sha256-hash token)
;          auth-system-id "password"
;          user-id (:id user)
;          check-query (-> (sql/select :*)
;                        (sql/from :user_sessions)
;                        (sql/where [:= :user_id [:cast user-id :uuid]]
;                          [:= :authentication_system_id auth-system-id])
;                        sql-format)
;          existing-session (jdbc/execute-one! (:tx request) check-query)]
