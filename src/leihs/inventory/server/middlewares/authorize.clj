(ns leihs.inventory.server.middlewares.authorize
  (:require
   [clojure.string :as str]
   [leihs.inventory.server.utils.authorize.main :refer [authorized-role-for-pool
                                                        AUTHORIZED-ROLES]]
   [leihs.inventory.server.utils.response-helper :as rh]
   [ring.util.response :as response]))

(defn unauthorized-response [request]
  (let [authenticated? (-> request :authenticated-entity boolean)
        json-request? (str/includes? (get-in request [:headers "accept"] "") "json")]
    (if json-request?
      ;; JSON request
      (if authenticated?
        ;; Authenticated but lacks permission -> 403
        (response/status (response/response {:status "failure" :message "Forbidden"}) 403)
        ;; Not authenticated -> 401
        (response/status (response/response {:status "failure" :message "Not authenticated"}) 401))
      ;; HTML request -> Always return SPA with 200
      (rh/index-html-response request 200))))

(defn wrap-authorize [handler]
  (fn [{:keys [authenticated-entity request-method]
        :as request}]
    (let [method request-method
          route-data (get-in request [:reitit.core/match :data method])
          public? (:public route-data)]
      (if (or public? authenticated-entity)
        (handler request)
        (unauthorized-response request)))))

(defn wrap-authorize-for-pool [handler]
  (fn [{{{pool-id :pool_id} :path} :parameters
        :keys [request-method]
        :as request}]
    (let [method request-method
          route-data (get-in request [:reitit.core/match :data method])
          public? (:public route-data)]

      (if public?
        (handler request)
        (let [role (authorized-role-for-pool request pool-id)]
          (if (contains? AUTHORIZED-ROLES role)
            (handler (assoc-in request [:authenticated-entity :role] role))
            (unauthorized-response request)))))))
