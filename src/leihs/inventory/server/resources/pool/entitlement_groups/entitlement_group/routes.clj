(ns leihs.inventory.server.resources.pool.entitlement-groups.entitlement-group.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.entitlement-groups.entitlement-group.main :as entitlement-group]
   [leihs.inventory.server.resources.pool.entitlement-groups.entitlement-group.types :as types]
   [leihs.inventory.server.resources.pool.entitlement-groups.types :refer [PosInt]]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(def get-doc
  "\n- quantity must be a positive integer
\n- available_count is computed based on current items in the pool (items_count) minus allocations in other entitlement groups
\n- items_count is the number of non-retired, borrowable, top-level items in the pool for the model")

(defn routes []
  ["/entitlement-groups/:entitlement_group_id"
   {:get {:summary (fe "a.k.a 'Anspruchsgruppe'")
          :description get-doc
          :accept "application/json"
          :coercion reitit.coercion.schema/coercion
          :parameters {:path {:pool_id s/Uuid
                              :entitlement_group_id s/Uuid}}
          :produces ["application/json"]
          :handler entitlement-group/get-resource
          :responses {200 {:description "OK"
                           :body types/get-response-body}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}

    :put {:summary (fe "a.k.a 'Anspruchsgruppen'")
          :description "Restrictions\n- quantity must be a positive integer\n- position is set to 0 by default
           \n- Processing a full sync\n- users accepts 'direct-entitlement'-entries only, no 'group-entitlement'-entries"
          :accept "application/json"
          :coercion reitit.coercion.schema/coercion
          :swagger {:produces ["application/json"]}
          :parameters {:path {:pool_id s/Uuid
                              :entitlement_group_id s/Uuid}
                       :body {:name s/Str
                              :is_verification_required s/Bool
                              :users [{:id s/Uuid}]
                              :groups [{:id s/Uuid}]
                              :models [{:id s/Uuid
                                        :quantity PosInt}]}}
          :produces ["application/json"]
          :handler entitlement-group/put-resource
          :responses {200 {:description "OK"
                           :body types/put-response-body}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}

    :delete {:summary (fe "a.k.a 'Anspruchsgruppen'")
             :accept "application/json"
             :coercion reitit.coercion.schema/coercion
             :swagger {:produces ["application/json"]}
             :parameters {:path {:pool_id s/Uuid
                                 :entitlement_group_id s/Uuid}}
             :produces ["application/json"]
             :handler entitlement-group/delete-resource
             :responses {200 {:description "OK"
                              :body types/delete-response-body}
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}}])
