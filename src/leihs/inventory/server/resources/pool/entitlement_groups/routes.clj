(ns leihs.inventory.server.resources.pool.entitlement-groups.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.entitlement-groups.main :as entitlement-groups]
   [leihs.inventory.server.resources.pool.entitlement-groups.types :as types]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn create-description [url]
  (str "- GET " url " Accept: application/json "))

(defn routes []
  ["/entitlement-groups/"
   {:get {:summary (fe "a.k.a 'Anspruchsgruppen'")
          :accept "application/json"
          :coercion reitit.coercion.schema/coercion
          :swagger {:produces ["application/json"]}
          :parameters {:path {:pool_id s/Uuid}
                       :query {(s/optional-key :page) s/Int
                               (s/optional-key :size) s/Int}}
          :produces ["application/json"]
          :handler entitlement-groups/index-resources
          :responses {200 {:description "OK"
                           :body types/get-response-body}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}

    :post {:summary (fe "a.k.a 'Anspruchsgruppen'")
           :description "Restrictions\n- quantity must be a positive integer\n- position is set to 0 by default"
           :accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :swagger {:produces ["application/json"]}
           :parameters {:path {:pool_id s/Uuid}
                        :body {:name s/Str
                               :is_verification_required s/Bool
                               :users [{:id s/Uuid}]
                               :groups [{:id s/Uuid}]
                               :models [{:id s/Uuid
                                         :quantity types/PosInt}]}}
           :produces ["application/json"]
           :handler entitlement-groups/post-resource
           :responses {200 {:description "OK"
                            :body types/post-response-body}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}])
