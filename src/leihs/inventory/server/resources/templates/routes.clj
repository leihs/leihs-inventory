(ns leihs.inventory.server.resources.templates.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.templates.main :refer [get-templates-of-pool-handler]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn get-templates-routes []
  [""
   ["/:pool_id"
    {:swagger {:conflicting true
               :tags ["Categories / Templates | Model-Groups"]
               :security []}}
    ["/templates"
     ["" {:get {:conflicting true
                :summary "Model-Groups of type 'Template' used for orders in borrow."
                :description ""
                :accept "application/json"
                :coercion reitit.coercion.schema/coercion
                :middleware [accept-json-middleware]
                :swagger {:produces ["application/json"]}
                :parameters {:path {:pool_id s/Uuid}}
                :handler get-templates-of-pool-handler
                :responses {200 {:description "OK"
                                 :body [{:id s/Uuid
                                         :name s/Str
                                         :type s/Str
                                         :created_at s/Any
                                         :updated_at s/Any}]}
                            404 {:description "Not Found"}
                            500 {:description "Internal Server Error"}}}}]
     ["/:template_id"
      {:get {:conflicting true
             :summary "OK | a.k.a 'Templates'"
             :accept "application/json"
             :coercion reitit.coercion.schema/coercion
             :middleware [accept-json-middleware]
             :swagger {:produces ["application/json"]}
             :parameters {:path {:pool_id s/Uuid :template_id s/Uuid}}
             :handler get-templates-of-pool-handler
             :responses {200 {:description "OK"
                              :body [{:id s/Uuid
                                      :name s/Str
                                      :type s/Str
                                      :created_at s/Any
                                      :updated_at s/Any}]}
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}}]]]])
