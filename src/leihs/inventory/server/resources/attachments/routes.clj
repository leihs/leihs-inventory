(ns leihs.inventory.server.resources.attachments.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.attachments.main :refer [get-attachments-handler]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-image-middleware accept-json-middleware]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn get-attachments-routes []
  ["/"
   {:swagger {:conflicting true
              :tags ["Attachments [v1]"]}}

   ["attachments/:id"
    {:get {:conflicting true
           :accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :swagger {:produces ["application/json" "application/octet-stream"]}
           :parameters {:path {:id s/Uuid}
                        :query {(s/optional-key :content_disposition) (s/enum "attachment" "inline")}}
           :handler get-attachments-handler
           :responses {200 {:description "OK"}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]

   ["attachments/"
    {:get {:conflicting true
           :accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :middleware [accept-json-middleware]
           :swagger {:produces ["application/json"]}
           :handler get-attachments-handler
           :responses {200 {:description "OK"}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]])
