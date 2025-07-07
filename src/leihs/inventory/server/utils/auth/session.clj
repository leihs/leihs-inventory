(ns leihs.inventory.server.utils.auth.session
  (:require
   [buddy.auth.backends.token :refer [jws-backend]]
   [buddy.auth.middleware :refer [wrap-authentication]]
   [buddy.core.codecs :refer [bytes->b64 bytes->str]]
   [buddy.core.hash :as hash]
   [cider-ci.open-session.bcrypt :refer [checkpw hashpw]]
   [clojure.walk :refer [keywordize-keys]]
   [cryptohash-clj.api :refer :all]
   [next.jdbc :as jdbc]))

(def LEIHS_SESSION_COOKIE_NAME :leihs-user-session)

(def expiration-sql-expr
  [:+
   :user_sessions.created_at
   [:*
    :authentication_systems.session_max_lifetime_hours
    [:raw "INTERVAL '1 hour'"]]])

(def selects
  [[:authentication_systems.id :auth_system_id]
   [:authentication_systems.name :auth_system_name]
   ;[:people.first_name :person_first_name]
   ;[:people.institutional_id :person_institutional_id]
   ;[:people.last_name :person_last_name]
   ;[:people.pseudonym :person_pseudonym]
   [:user_sessions.created_at :session_created_at]
   [:user_sessions.id :session_id]
   [:users.email :user_email]
   [:users.id :user_id]
   [:users.institutional_id :user_institutional_id]
   [:users.login :user_login]
   [expiration-sql-expr :session_expires_at]])

(defn user-session [token tx]

  (let [sql-query ["SELECT
            authentication_systems.id AS auth_system_id,
            authentication_systems.name AS auth_system_name,
            user_sessions.created_at AS session_created_at,
            user_sessions.id AS session_id,
            users.email AS user_email,
            users.id AS user_id,
            users.login AS user_login
          FROM user_sessions
          INNER JOIN users ON user_sessions.user_id = users.id
          INNER JOIN authentication_systems_users
            ON user_sessions.authentication_system_id = authentication_systems_users.authentication_system_id
          INNER JOIN authentication_systems
            ON authentication_systems_users.authentication_system_id = authentication_systems.id
            WHERE token_hash=encode(digest(?, 'sha256'), 'hex')"
                   token]

        res (jdbc/execute-one! tx sql-query)]
    res))

(defn get-cookie-value
  ([request]
   (-> request keywordize-keys :cookies
       LEIHS_SESSION_COOKIE_NAME :value))
  ([request key]
   (-> request keywordize-keys :cookies
       key :value)))

(defn find-user-by-id [tx user-id]
  (jdbc/execute-one! tx ["SELECT * FROM users WHERE id = ?" user-id]))

(defn is-admin [user-id tx]
  (let [user (find-user-by-id tx user-id)]
    (boolean (:is_admin user))))

(defn- handle [request handler]
  (if-let [token (get-cookie-value request)]
    (let [tx (:tx request)]

      (if-let [user-session (user-session token tx)]
        (let [user-id (:user_id user-session)
              expires-at (:session_expires_at user-session)
              user (find-user-by-id tx user-id)
              user (assoc user :type "User")]

          (handler
           (assoc request
                  :authenticated-entity user
                  :is_admin (is-admin user-id tx)
                  :authentication-method "Session"
                  :session-expires-at expires-at)))
        {:status 401 :body {:message "The session is invalid or expired!"}}))
    (handler request)))

(defn wrap-session-authorize! [handler]
  (fn [request]
    (handle request handler)))

;### Debug ####################################################################
;(debug/debug-ns *ns*)
