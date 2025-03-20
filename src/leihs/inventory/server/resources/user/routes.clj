(ns leihs.inventory.server.resources.user.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.user.main :refer [get-pools-of-user-handler get-user-details-handler get-user-profile]]
   [leihs.inventory.server.resources.utils.flag :as i]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware wrap-authenticate! wrap-is-admin!]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(def schema-min
  {:id s/Uuid
   :name s/Str
   (s/optional-key :description) (s/maybe s/Str)})

(defn get-user-routes []
  ["/"
   {:swagger {:conflicting true
              :tags ["User"] :security []}}

   ["pools"
    [""
     {:get {:conflicting true
            :summary (i/session "Get pools of the authenticated user.")
            :accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :middleware [wrap-authenticate! accept-json-middleware]
            :swagger {:produces ["application/json"]}
            :handler get-pools-of-user-handler
            :responses {200 {:description "OK"
                             ;:body [schema-min] ;; FIXME
                             }
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]

    ["/:user_id" {:get {:conflicting true
                        :summary (i/admin "Get pools of any user.")
                        :coercion reitit.coercion.schema/coercion
                        :accept "application/json"
                        :middleware [wrap-is-admin! accept-json-middleware]
                        :swagger {:produces ["application/json"]
                                  :deprecated true}
                        :parameters {:path {:user_id s/Uuid}}
                        :handler get-pools-of-user-handler
                        :responses {200 {:description "OK"
                                         ;:body [schema-min] ;; FIXME: merge with "user-pools-info"
                                         }
                                    404 {:description "Not Found"}
                                    500 {:description "Internal Server Error"}}}}]]

   ["profile"
    {:get {:conflicting true
           :accept "application/json"
           :summary (i/session "Get details of the authenticated user.")
           :coercion reitit.coercion.schema/coercion
           :middleware [wrap-authenticate! accept-json-middleware]
           :swagger {:produces ["application/json"]}
           :handler get-user-profile
           :responses {200 {:description "OK"
                            :body s/Any}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]

   ["details"
    [""
     {:get {:conflicting true
            :accept "application/json"
            :summary (i/session "Get details of the authenticated user.")
            :coercion reitit.coercion.schema/coercion
            :middleware [wrap-authenticate! accept-json-middleware]
            :swagger {:produces ["application/json"]
                      :deprecated true}
            :handler get-user-details-handler
            :responses {200 {:description "OK"
                             :body s/Any}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]

    ["/:user_id"
     {:get {:conflicting true
            :summary (i/admin "Get details of any user.")
            :accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :middleware [wrap-is-admin! accept-json-middleware]
            :swagger {:produces ["application/json"]}
            :parameters {:path {:user_id s/Uuid}}
            :handler get-user-details-handler
            :responses {200 {:description "OK"
                             :body s/Any}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]]])

