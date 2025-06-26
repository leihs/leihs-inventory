(ns leihs.inventory.server.resources.dev.routes
  (:require
   [clojure.set]

   [leihs.inventory.server.resources.utils.flag :as i]
   [leihs.inventory.server.resources.utils.middleware :refer [wrap-is-admin!]]

   [leihs.core.status :as status]
   [leihs.inventory.server.constants :refer [HIDE_BASIC_ENDPOINTS APPLY_DEV_ENDPOINTS]]

   [leihs.core.auth.session :refer [wrap-authenticate]]
   [leihs.inventory.server.resources.dev.main :refer [run-get-views
                                                      search-in-tables
                                                      update-and-fetch-accounts]]
   [leihs.inventory.server.utils.auth.inventory-auth :refer [wrap-check-authenticated-admin]]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]

   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware wrap-is-admin! wrap-authenticate!]]

   [leihs.inventory.server.resources.user.main :refer [get-pools-of-user-handler get-pools-access-rights-of-user-handler
                                                       get-user-profile get-user-details-handler]]


   ;[cheshire.core :as json]
   ;[clojure.java.io :as io]
   ;[clojure.string :as str]
   ;[leihs.core.anti-csrf.back :refer [anti-csrf-props anti-csrf-token]]
   ;[leihs.core.auth.session :refer [wrap-authenticate]]
   ;[leihs.core.constants :as constants]
   ;[leihs.core.sign-in.back :as be]
   ;[leihs.core.sign-in.simple-login :refer [sign-in-view]]
   ;[leihs.core.sign-out.back :as so]
   ;[leihs.core.status :as status]
   ;[leihs.inventory.server.constants :as consts :refer [HIDE_BASIC_ENDPOINTS HIDE_DEV_ENDPOINTS]]
   ;[leihs.inventory.server.resources.attachments.routes :refer [get-attachments-routes]]
   [leihs.inventory.server.resources.auth.auth-routes :refer [authenticate-handler logout-handler set-password-handler
                                                              update-role-handler
                                                              ;token-routes
                                                              session-token-routes]]
   ;[leihs.inventory.server.resources.auth.session :as ab]
   ;[leihs.inventory.server.resources.buildings_rooms.routes :refer [get-buildings-rooms-routes]]
   ;[leihs.inventory.server.resources.categories.routes :refer [get-categories-routes]]
   ;[leihs.inventory.server.resources.dev.routes :refer [get-dev-routes]]
   ;[leihs.inventory.server.resources.export.routes :refer [get-export-routes]]
   ;[leihs.inventory.server.resources.fields.routes :refer [get-fields-routes]]
   ;[leihs.inventory.server.resources.images.routes :refer [get-images-routes]]
   ;[leihs.inventory.server.resources.items.routes :refer [get-items-routes]]
   ;[leihs.inventory.server.resources.models.routes :refer [get-model-by-pool-route get-model-route]]
   ;[leihs.inventory.server.resources.models.tree.routes :refer [get-tree-route]]
   ;[leihs.inventory.server.resources.owner-department.routes :refer [get-owner-department-routes]]
   ;[leihs.inventory.server.resources.pools.routes :refer [get-pools-routes]]
   ;[leihs.inventory.server.resources.properties.routes :refer [get-properties-routes]]
   ;[leihs.inventory.server.resources.supplier.routes :refer [get-supplier-routes]]
   ;[leihs.inventory.server.resources.user.routes :refer [get-user-routes]]
   ;[leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware wrap-is-admin! restrict-uri-middleware]]
   ;[leihs.inventory.server.utils.helper :refer [convert-to-map]]
   ;[leihs.inventory.server.utils.html-utils :refer [add-csrf-tags]]
   ;[muuntaja.core :as m]
   ;[reitit.coercion.schema]
   ;[reitit.coercion.spec]
   ;[reitit.openapi :as openapi]
   ;[reitit.ring.middleware.muuntaja :as muuntaja]
   ;[reitit.swagger :as swagger]
   ;[ring.util.response :as response]
   ;[ring.util.response :refer [bad-request response status]]

   [schema.core :as s]))


(def update-role-response {:role-before s/Str
                           :role-after s/Str
                           :inventory_pool_id s/Uuid
                           :count-of-direct-access-right-should-be-one s/Int
                           (s/optional-key :update-result) s/Any})

(defn get-dev-routes []

  [""


    ["/admin/status"
     {:get {:accept "application/json"
            :handler status/status-handler
            :middleware [wrap-is-admin!]}}]

   ["set-password"
    {:post {:summary "OK | Set password by basicAuth for already authenticated user"
            :accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :parameters {:body {:new-password1 s/Str}}
            :handler set-password-handler}}]

   ["admin/update-role"
    {:put {:summary "[] OK | DEV | Update direct-user-role [v0]"
           :accept "application/json"
           :description "- default pool-id: 8bd16d45-056d-5590-bc7f-12849f034351"
           :parameters {:query {:role (s/enum "inventory_manager" "lending_manager" "group_manager" "customer")
                                (s/optional-key :pool_id) s/Uuid}}
           :coercion reitit.coercion.schema/coercion
           :middleware [wrap-is-admin!]
           :handler update-role-handler
           :responses {200 {:description "OK" :body update-role-response}
                       409 {:description "Conflict" :body update-role-response}
                       500 {:description "Internal Server Error"}}}}]


   ["/dev"
    {:swagger {:conflicting true
               :tags ["Dev"]}
     :no-doc (not APPLY_DEV_ENDPOINTS)
     }

    ;;; TODO: move to DEV?
    ;["/pools"
    ; {:get {:conflicting true
    ;        :summary (i/session "Get pools of the authenticated user.")
    ;        :accept "application/json"
    ;        :coercion reitit.coercion.schema/coercion
    ;        :middleware [wrap-authenticate! accept-json-middleware]
    ;        :swagger {:produces ["application/json"]}
    ;        :handler get-pools-of-user-handler
    ;        :responses {200 {:description "OK"
    ;                         ;:body [schema-min] ;; FIXME
    ;                         }
    ;                    404 {:description "Not Found"}
    ;                    500 {:description "Internal Server Error"}}}}]

    ["/update-accounts" {:put {:conflicting true
                               :summary "Overwrite pw for accounts with various roles OR is_admin"
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
                               :handler update-and-fetch-accounts
                               :responses {200 {:description "OK"
                                                :body s/Any}
                                           404 {:description "Not Found"}
                                           500 {:description "Internal Server Error"}}}}]

    ["/usage" {:get {:conflicting true
                     :summary "Used to determine appearance of uuid in tables"
                     :accept "application/json"
                     :middleware [wrap-check-authenticated-admin]
                     :coercion reitit.coercion.schema/coercion
                     :parameters {:query {(s/optional-key :id) s/Str
                                          (s/optional-key :columns) [s/Str]}}
                     :handler search-in-tables
                     :responses {200 {:description "OK"
                                      :body s/Any}
                                 404 {:description "Not Found"}
                                 500 {:description "Internal Server Error"}}}}]]])
