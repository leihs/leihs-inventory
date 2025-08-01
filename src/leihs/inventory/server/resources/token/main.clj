(ns leihs.inventory.server.resources.token.main
  (:require
   [cider-ci.open-session.bcrypt :refer [hashpw]]
   [next.jdbc :as jdbc]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.util.response :as response]
   [taoensso.timbre :refer [error]])
  (:import
   (com.google.common.io BaseEncoding)))

(def b32 (BaseEncoding/base32))

(defn secret [n]
  (->> n crypto.random/bytes
       (.encode b32)
       (map char)
       (apply str)))

(defn create-api-token [request user-id scopes description]
  (let [full-token (secret 20)
        token-part (subs full-token 0 5)
        hashed-token (hashpw full-token)

        now-raw (java.time.Instant/now)
        now (java.sql.Timestamp/from now-raw)

        expires-at (.plus now-raw (java.time.Duration/ofDays 30))
        expires-sql (java.sql.Timestamp/from expires-at)

        data ["INSERT INTO api_tokens
                     (user_id, token_hash, token_part, scope_read, scope_write, scope_admin_read, scope_admin_write,
                      description, created_at, updated_at, expires_at)
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
              user-id
              hashed-token
              token-part
              (:read scopes)
              (:write scopes)
              (:admin_read scopes)
              (:admin_write scopes)
              description
              now
              now
              expires-sql]

        res (try (jdbc/execute-one! (:tx request) data)
                 (catch Exception e (error "Error inserting token:" e) nil))]
    {:token full-token
     :expires_at expires-at
     :scopes scopes}))

(defn post-resource [request]
  (let [user (-> request :authenticated-entity)
        {:keys [description scopes]} (:body-params request)
        user_id (:id user)
        scopes (merge {:read true :write false :admin_read false :admin_write false} scopes)]

    (if user_id
      (let [result (create-api-token request user_id scopes description)]
        (response/response
         {:status "success"
          :token (:token result)
          :expires_at (:expires_at result)
          :scopes scopes}))
      (response/status
       (response/response {:status "failure" :message "Invalid or missing credentials"})
       401))))
