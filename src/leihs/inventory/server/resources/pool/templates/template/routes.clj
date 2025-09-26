(ns leihs.inventory.server.resources.pool.templates.template.routes
  (:require
   [leihs.inventory.server.resources.pool.templates.template.main :as template]
   [leihs.inventory.server.resources.pool.templates.template.types :as types]
   [leihs.inventory.server.resources.pool.templates.types]
   [reitit.coercion.schema]
   [reitit.coercion.spec :as spec]
   [ring.middleware.accept]))

(defn routes []
  ["/templates/:template_id"
   {:get {:accept "application/json"
          :coercion spec/coercion
          :description "- 'vailable' means having items that are borrowable and not retired
\n- template_id == group_id"
          :parameters {:path {:pool_id uuid?
                              :template_id uuid?}}
          :handler template/get-resource
          :produces ["application/json"]
          :responses {200 {:description "OK"
                           :body ::types/get-put-response}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}

    :put {:accept "application/json"
          :coercion spec/coercion
          :description "- Update template by processing a Full-Sync
\n- 'available' means having items that are borrowable and not retired
\n- Templates without models are not allowed
\n- template_id == group_id"
          :parameters {:path {:pool_id uuid?
                              :template_id uuid?}
                       :body ::types/put-query}
          :handler template/put-resource
          :produces ["application/json"]
          :responses {200 {:description "OK"
                           :body ::types/get-put-response}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}

    :delete {:accept "application/json"
             :coercion spec/coercion
             :description "- template_id == group_id"
             :parameters {:path {:pool_id uuid?
                                 :template_id uuid?}}
             :handler template/delete-resource
             :produces ["application/json"]
             :responses {200 {:description "OK"
                              :body ::types/delete-response}
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}}])
