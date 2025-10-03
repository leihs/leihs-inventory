(ns leihs.inventory.server.middlewares.authorize
  (:require
   [clojure.string :as str]
   [leihs.core.core :refer [detect]]
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
        (let [access-right (detect #(= (:inventory_pool_id %) pool-id)
                                   (get-in request [:authenticated-entity :access-rights]))
              role (:role access-right)]
          (if (contains? #{"lending_manager" "inventory_manager"} role)
            (handler (assoc-in request [:authenticated-entity :role] role))
            (unauthorized-response request)))))))