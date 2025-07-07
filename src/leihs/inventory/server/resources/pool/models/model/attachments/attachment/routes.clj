(ns leihs.inventory.server.resources.pool.models.model.attachments.attachment.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.models.model.attachments.attachment.main :refer [get-attachments-handler
                                                                                           delete-attachments]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-image-middleware accept-json-middleware]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn get-models-model-attachments-single-routes []
  ["/:pool_id/"
   {:swagger {:tags [""]}}

   ["models/:model_id/attachments/:attachments_id"
    {:get {:summary (fe "")
           :accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :swagger {:produces ["application/json" "application/octet-stream"]}
           :parameters {:path {:pool_id s/Uuid
                               :model_id s/Uuid
                               :attachments_id s/Uuid}
                        :query {(s/optional-key :content_disposition) (s/enum "attachment" "inline")}}
           :handler get-attachments-handler
           :responses {200 {:description "OK"}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}

     :delete {:accept "application/json"
              :summary (fe "")
              :coercion reitit.coercion.schema/coercion
              :parameters {:path {:pool_id s/Uuid
                                  :model_id s/Uuid
                                  :attachments_id s/Uuid}}
              :handler delete-attachments
              :responses {200 {:description "OK"}
                          404 {:description "Not Found"}
                          500 {:description "Internal Server Error"}}}}]])
