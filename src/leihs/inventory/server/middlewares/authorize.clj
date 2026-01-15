(ns leihs.inventory.server.middlewares.authorize
  (:require
   [clojure.string :as str]
   [leihs.inventory.server.middlewares.authorize.main :refer [authorized-role-for-pool
                                                              AUTHORIZED-ROLES]]
   [leihs.inventory.server.utils.response :as rh]
   [ring.util.response :as response]))

(def supported-accepts
  #{"text/html"
    "application/json"
    "image/"
    "text/csv"
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"})

(defn unauthorized-response [request]
  (let [authenticated? (-> request :authenticated-entity boolean)
        accept (str/lower-case (get-in request [:headers "accept"] ""))
        json-request? (str/includes? accept "json")
        html-request? (or (str/includes? accept "text/html") (str/includes? accept "*/*"))
        image-request? (str/includes? accept "image/")
        supported? (or (= accept "*/*")
                       (some #(str/includes? accept %) supported-accepts))]
    (cond
      ;; Unsupported Accept header → 406
      (not supported?)
      (response/status (response/response "Not Acceptable") 406)

      ;; JSON or image or other non-HTML requests
      (or json-request? image-request? (not html-request?))
      (if authenticated?
        ;; Authenticated but lacks permission -> 403
        (response/status (response/response {:status "failure" :message "Forbidden"}) 403)
        ;; Not authenticated -> 401
        (response/status (response/response {:status "failure" :message "Not authenticated"}) 401))

      ;; HTML request -> Always return SPA with 200
      :else
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
