(ns leihs.inventory.server.middlewares.authorize
  (:require
   [clojure.string :as str]
   [leihs.inventory.server.utils.authorize.main :refer [AUTHORIZED-ROLES
                                                        authorized-role-for-pool]]
   [ring.util.response :as response]))

(defn unauthorized-response [request]
  (if (str/includes? (get-in request [:headers "accept"] "") "json")
    (response/status (response/response {:status "failure" :message "Unauthorized"}) 403)
    {:status 302
     :headers {"Location" "/sign-in?return-to=%2Finventory"
               "Content-Type" "text/html"}
     :body ""}))

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
