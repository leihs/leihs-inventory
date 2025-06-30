(ns leihs.inventory.server.resources.pool.models.model.items.routes
  (:require
   [clojure.spec.alpha :as sa]
   [clojure.string :as str]
   [leihs.inventory.server.resources.pool.models.coercion :as mc]

   [leihs.inventory.server.resources.pool.models.model.items.main :refer [get-items-with-pagination-handler]]

   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.auth.role-auth :refer [permission-by-role-and-pool]]
   [leihs.inventory.server.utils.auth.roles :as roles]
   [leihs.inventory.server.utils.coercion.core :refer [Date]]
   [reitit.coercion.schema]
   [reitit.coercion.spec :as spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn get-models-single-items-route []
  ["/:pool_id"

   {:swagger {:conflicting true
              :tags []}}

;; Routes for /inventory/<pool-id>/*
   ;; TODO: should be? ["/models/list"
   ["/models/:model_id"

    ["/items"
     ["" {:get {:accept "application/json"
                :coercion reitit.coercion.schema/coercion
                :middleware [accept-json-middleware]
                :swagger {:produces ["application/json"]}
                :parameters {:path {:pool_id s/Uuid
                                    :model_id s/Uuid}}
                :handler get-items-with-pagination-handler
                :responses {200 {:description "OK"
                                 ;:body (s/->Either [s/Any schema])} ;;FIXME
                                 :body s/Any}

                            404 {:description "Not Found"}
                            500 {:description "Internal Server Error"}}}}]

     ["/:item_id"
      {:get {:accept "application/json"
             :coercion reitit.coercion.schema/coercion
             :middleware [accept-json-middleware]
             :swagger {:produces ["application/json"]}
             :parameters {:path {:pool_id s/Uuid
                                 :model_id s/Uuid
                                 :item_id s/Uuid}}
             :handler get-items-with-pagination-handler
             :responses {200 {:description "OK"
                              ;:body (s/->Either [s/Any schema])} ;;FIXME
                              :body s/Any}

                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}}]]]])
