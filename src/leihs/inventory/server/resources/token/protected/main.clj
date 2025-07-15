(ns leihs.inventory.server.resources.token.protected.main
  (:require
   [crypto.random]
   [cryptohash-clj.api :refer :all]
   [digest :as d]
   [leihs.inventory.server.resources.utils.request :refer [AUTHENTICATED_ENTITY authenticated? get-auth-entity]]
   [next.jdbc :as jdbc]
   [ring.util.response :as response]
   [schema.core :as s]
   [taoensso.timbre :refer [debug info warn error spy]])
  (:import (com.google.common.io BaseEncoding)
           (java.time Duration Instant)
           (java.util Base64 UUID)))

(defn get-resource [request]
  (if (authenticated? request)
    (do
      (debug "User authenticated with:" (get-auth-entity request))
      {:status 200
       :body {:message "Access granted to protected resource"
              :token (get-auth-entity request)}})
    (do
      (debug "User not authenticated")
      {:status 403 :body "Forbidden"})))

(defn extract-scope-attributes [data]
  (select-keys data (filter #(clojure.string/starts-with? (name %) "scope_") (keys data))))

(defn verify-token
  "Checks if the token is valid based on a database entry."
  [tx token]
  (let [now-raw (java.time.Instant/now)
        current-time (java.sql.Timestamp/from now-raw)
        query-result (if (not (nil? token))
                       (let [token-part (subs token 0 5)
                             query-result (jdbc/execute-one! tx
                                                             ["SELECT * FROM api_tokens WHERE token_part = ? AND expires_at > ?"
                                                              token-part
                                                              current-time])]
                         query-result)
                       nil)

        query-result (when (not (nil? query-result))
                       (let [token_hash (:token_hash query-result)
                             res (verify-with :bcrypt token token_hash)]
                         (if res
                           query-result
                           nil)))]
    (if query-result
      {:id (:user_id query-result)
       :scopes (extract-scope-attributes query-result)
       :expires_at (:expires_at query-result)}
      nil)))

(defn wrap-token-authentication
  "Middleware that checks if the token is valid."
  [handler]
  (fn [request]
    (let [tx (:tx request)
          header (get-in request [:headers "authorization"])
          token (when header (clojure.string/replace header "Token " ""))

          verification-result (verify-token tx token)]
      (if verification-result
        (handler (assoc request AUTHENTICATED_ENTITY verification-result))
        (response/status (response/response {:status "failure" :message "Unauthorized"}) 401)))))
