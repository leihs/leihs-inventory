(ns leihs.inventory.server.middlewares.authorize
  (:require
   [clojure.string :as str]
   [leihs.core.core :refer [detect]]
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
        (let [access-right (detect #(= (:inventory_pool_id %) pool-id)
                                   (get-in request [:authenticated-entity :access-rights]))
              role (:role access-right)]
          (if (contains? #{"lending_manager" "inventory_manager"} role)
            (handler (assoc-in request [:authenticated-entity :role] role))
            (unauthorized-response request)))))))