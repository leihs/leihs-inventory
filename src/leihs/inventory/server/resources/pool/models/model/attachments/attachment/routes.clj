(ns leihs.inventory.server.resources.pool.models.model.attachments.attachment.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.pool.attachments.constants :refer [ACCEPT_TYPES_ATTACHMENT]]
   [leihs.inventory.server.resources.pool.attachments.shared :as attachment]
   [leihs.inventory.server.resources.pool.attachments.types :refer [error-attachment-not-found
                                                                    get-attachment-response]]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  ["/models/:model_id/attachments/:attachment_id"
   {:get {:coercion reitit.coercion.schema/coercion
          :swagger {:produces ACCEPT_TYPES_ATTACHMENT}
          :produces ACCEPT_TYPES_ATTACHMENT
          :parameters {:path {:pool_id s/Uuid
                              :model_id s/Uuid
                              :attachment_id s/Uuid}
                       :query {(s/optional-key :content_disposition) (s/enum "attachment" "inline")}}
          :handler attachment/get-resource
          :responses {200 {:description "OK"
                           :body get-attachment-response}
                      404 {:description "Not Found"
                           :body error-attachment-not-found}
                      406 {:description "Requested content type not supported"}
                      500 {:description "Internal Server Error"}}}

    :delete {:accept "application/json"
             :coercion reitit.coercion.schema/coercion
             :parameters {:path {:pool_id s/Uuid
                                 :model_id s/Uuid
                                 :attachment_id s/Uuid}}
             :produces ["application/json"]
             :handler attachment/delete-resource
             :responses {200 {:description "OK"
                              :body s/Any}
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}}])
