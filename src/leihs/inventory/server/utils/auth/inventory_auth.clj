(ns leihs.inventory.server.utils.auth.inventory-auth
  (:require
   [dev.main :refer [extract-basic-auth-from-header verify-password-entry]]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [next.jdbc :as jdbc]
   [ring.util.response :as response]
   [taoensso.timbre :refer [debug info warn error spy]]))

(def CONST_LOG_AUTH false)

(defn log-auth-details [authenticated-entity]
  (when CONST_LOG_AUTH
    (let [user (:login authenticated-entity)
          email (:email authenticated-entity)
          auth-meth (:authentication-method authenticated-entity)]
      (debug (str "> Authentication: " user " / " email ", authentication-method: " (name auth-meth))))))

(defn access-rights [tx user-id]
  (-> (sql/select :role :inventory_pool_id)
      (sql/from :access_rights)
      (sql/where [:= :user_id user-id])
      sql-format
      (->> (jdbc/execute-one! tx))))

(defn authenticated-user-entity [{tx :tx :as request}]
  (let [[login password] (extract-basic-auth-from-header request)
        user (verify-password-entry request login password)]
    (if user
      (let [auth (assoc user
                        :authentication-method :basicAuth
                        :access-rights (access-rights tx (:id user))
                        :scope_read true
                        :scope_write true
                        :scope_admin_read (:is_admin user)
                        :scope_admin_write (:is_admin user)
                        :scope_system_admin_read (:is_system_admin user)
                        :scope_system_admin_write (:is_system_admin user))]
        (assoc request :authenticated-entity auth))
      request)))

(defn wrap-check-auth
  [handler check-admin?]
  (fn [request]
    (let [request-with-auth (assoc request :authenticated-entity (authenticated-user-entity request))
          authenticated-entity (or (:authenticated-entity request)
                                   (:authenticated-entity (authenticated-user-entity request)))]
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
