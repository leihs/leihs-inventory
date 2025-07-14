(ns leihs.inventory.server.resources.pool.models.model.attachments.routes
  (:require
   [clojure.spec.alpha :as sa]
   [clojure.string :as str]
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.models.model.attachments.main :refer [post-resource
                                                                                index-resources]]
   [leihs.inventory.server.resources.pool.models.model.attachments.types :refer [get-attachments-response
                                                                                 attachment]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.auth.role-auth :refer [permission-by-role-and-pool]]
   [leihs.inventory.server.utils.auth.roles :as roles]
   [leihs.inventory.server.utils.coercion.core :refer [Date]]
   [leihs.inventory.server.utils.constants :refer [config-get]]
   [reitit.coercion.schema]
   [reitit.coercion.spec :as spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn get-models-model-attachments-route []
  ["/"
   {:swagger {:tags [""]}}

   [":pool_id/models"

    ["/:model_id"
     ["/attachments"
      ["/"
       {:get {:accept "application/json"
              :coercion reitit.coercion.schema/coercion
              :middleware [accept-json-middleware]
              :swagger {:produces ["application/json"]}
              :parameters {:path {:pool_id s/Uuid
                                  :model_id s/Uuid}
                           :query {(s/optional-key :page) s/Int
                                   (s/optional-key :size) s/Int}}
              :handler index-resources
              :responses {200 {:description "OK"
                               :body get-attachments-response}
                          404 {:description "Not Found"}
                          500 {:description "Internal Server Error"}}}

        :post {:accept "application/json"
               :summary (fe "")
               :description (str "- Limitations: " (config-get :api :attachments :max-size-mb) " MB\n"
                                 "- Allowed File types: " (str/join ", " (config-get :api :attachments :allowed-file-types)))
               :coercion reitit.coercion.schema/coercion
               :middleware [accept-json-middleware]
               :swagger {:produces ["application/json"]}
               :parameters {:path {:pool_id s/Uuid
                                   :model_id s/Uuid}
                            :header {:x-filename s/Str}}
               :handler post-resource
               :responses {200 {:description "OK"
                                :body s/Any}
                           400 {:description "Bad Request (Coercion error)"
                                :body s/Any}
                           404 {:description "Not Found"}
                           500 {:description "Internal Server Error"}}}}]]]]])
