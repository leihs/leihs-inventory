(ns leihs.inventory.server.middlewares.authorize
  (:require
   [leihs.core.core :refer [detect]]
   [ring.util.response :as response]
   [clojure.string :as str]
   [leihs.inventory.server.utils.response_helper :as rh]
   [taoensso.timbre :as timbre :refer [debug spy]]))


(defn unauthorized-response [request]
       (if (str/includes? (get-in request [:headers "accept"] "") "json")
         (response/status (response/response {:status "failure" :message "Unauthorized.j"}) 403)
         {:status 302
          :headers {"Location" "/sign-in?return-to=%2Finventory"
                    "Content-Type" "text/html"}
          :body ""}))


(defn wrap-authorize [handler]
  (fn [{{{pool-id :pool_id} :path} :parameters
        :keys [authenticated-entity request-method]
        :as request}]
    (let [method        request-method
          route-data    (get-in request [:reitit.core/match :data method])
          public?       (:public route-data)
          ]

      (println ">o> abc.authenticated-entity ??" authenticated-entity)
      (println ">o> abc.route-is-public?" public?)

      (if (or public? authenticated-entity)
        (handler request)
        (unauthorized-response request))

      ;(unauthorized-response request)
      )))


(defn wrap-authorize-for-pool [handler]
  (fn [{{{pool-id :pool_id} :path} :parameters
        :keys [authenticated-entity request-method]
        :as request}]
    (let [method     request-method
          route-data (get-in request [:reitit.core/match :data method])
          public?    (:public route-data)]

      (println ">o> abc.route-is-public?" public?)

      (if public?
        ;; route is marked public — skip access check
        (handler request)

        ;; otherwise require correct role for pool
        (let [access-right (detect #(= (:inventory_pool_id %) pool-id)
                             (get-in request [:authenticated-entity :access-rights]))
              role (:role access-right)]

          (println ">o> abc.role" role)

          (if (contains? #{"lending_manager" "inventory_manager"} role)
            (handler (assoc-in request [:authenticated-entity :role] role))

            ;{:status 403
            ; :body "Unauthorized due to insufficient access right role."}

            (unauthorized-response request)

            ))))))
