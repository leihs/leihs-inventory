(ns leihs.inventory.server.middlewares.authorize
  (:require
   [clojure.string :as str]
   [leihs.inventory.server.utils.authorize.main :refer [authorized-role-for-pool
                                                        AUTHORIZED-ROLES]]
   [ring.util.codec :as codec]
   [ring.util.response :as response]))

(defn unauthorized-response [request]
  (if (str/includes? (get-in request [:headers "accept"] "") "json")
    (response/status (response/response {:status "failure" :message "Unauthorized"}) 403)
    (let [uri (:uri request)
          query-string (:query-string request)
          full-url (if query-string
                     (str uri "?" query-string)
                     uri)
          encoded-url (codec/url-encode full-url)
          redirect-url (str "/sign-in?return-to=" encoded-url)]
      {:status 302
       :headers {"Location" redirect-url
                 "Content-Type" "text/html"}
       :body ""})))

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
