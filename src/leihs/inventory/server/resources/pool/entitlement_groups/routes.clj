(ns leihs.inventory.server.resources.pool.entitlement-groups.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.entitlement-groups.main :as entitlement-groups]
   [leihs.inventory.server.resources.pool.entitlement-groups.types :refer [response-body]]
   [leihs.inventory.server.utils.middleware :refer [accept-json-middleware]]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn create-description [url]
  (str "- GET " url " Accept: application/json "))

(defn routes []
   ["/entitlement-groups/"
   {:get {:summary (fe "a.k.a 'Anspruchsgruppen'")
                :description (create-description "https://staging.leihs.zhdk.ch/manage/8bd16d45-056d-5590-bc7f-12849f034351/groups")
                :accept "application/json"
                :coercion reitit.coercion.schema/coercion
                :middleware [accept-json-middleware]
                :swagger {:produces ["application/json"]}
                :parameters {:path {:pool_id s/Uuid}}
                :handler entitlement-groups/index-resources
                :responses {200 {:description "OK"
                                 :body [response-body]}
                            404 {:description "Not Found"}
                            500 {:description "Internal Server Error"}}}}])
