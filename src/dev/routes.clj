(ns dev.routes
  (:require
   [clojure.set]
   [dev.inventory-auth :refer [wrap-check-authenticated-admin]]
   [dev.main :refer [get-resource put-account-resource put-role-resource]]
   [leihs.inventory.server.constants :refer [APPLY_DEV_ENDPOINTS fe]]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [ring.util.response :as response]
   [schema.core :as s]))

(defn wrap-is-admin! [handler]
  (fn [request]
    (let [is-admin (get-in request [:authenticated-entity :is_admin] false)]
      (if is-admin
        (handler request)
        (response/status (response/response {:status "failure" :message "Unauthorized"}) 401)))))

(def update-role-response {:role-before s/Str
                           :role-after s/Str
                           :inventory_pool_id s/Uuid
                           :count-of-direct-access-right-should-be-one s/Int
                           (s/optional-key :update-result) s/Any})

(defn get-dev-routes []
  ["/dev"
   {:no-doc (not APPLY_DEV_ENDPOINTS)}

   ["update-role"
    {:put {:summary (fe "Update direct-user-role")
           :accept "application/json"
           :description "- default pool-id: 8bd16d45-056d-5590-bc7f-12849f034351"
           :parameters {:query {:role (s/enum "inventory_manager" "lending_manager" "group_manager" "customer")
                                (s/optional-key :pool_id) s/Uuid}}
           :coercion reitit.coercion.schema/coercion
           :middleware [wrap-is-admin!]
           :handler put-role-resource
           :responses {200 {:description "OK" :body update-role-response}
                       409 {:description "Conflict" :body update-role-response}
                       500 {:description "Internal Server Error"}}}}]

   ["/update-accounts"
    {:put {:summary "Overwrite pw for accounts with various roles OR is_admin"
           :description "Fetch one account of each variant of:
- role: inventory_manager, lending_manager, group_manager, customer\n
- is_admin: true\n
- inventory_pool_id=8bd16d45-056d-5590-bc7f-12849f034351\n
- is_system_admin: true\n\n
.. and set password"
           :accept "application/json"
           :middleware [wrap-check-authenticated-admin]
           :coercion reitit.coercion.schema/coercion
           :parameters {:query {(s/optional-key :type) (s/enum "min" "all")}}
           :handler put-account-resource
           :responses {200 {:description "OK"
                            :body s/Any}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]

   ["/usage" {:get {:summary "Used to determine appearance of uuid in tables"
                    :accept "application/json"
                    :middleware [wrap-check-authenticated-admin]
                    :coercion reitit.coercion.schema/coercion
                    :parameters {:query {(s/optional-key :id) s/Str
                                         (s/optional-key :columns) [s/Str]}}
                    :handler get-resource
                    :responses {200 {:description "OK"
                                     :body s/Any}
                                404 {:description "Not Found"}
                                500 {:description "Internal Server Error"}}}}]])
