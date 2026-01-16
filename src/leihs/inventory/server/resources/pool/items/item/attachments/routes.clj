(ns leihs.inventory.server.resources.pool.items.item.attachments.routes
  (:require
   [clojure.string :as str]
   [leihs.inventory.server.resources.pool.attachments.constants :refer [config-get]]
   [leihs.inventory.server.resources.pool.attachments.shared :refer [index-resources
                                                                     post-resource]]
   [leihs.inventory.server.resources.pool.attachments.types :refer [attachment-response
                                                                    get-attachments-response]]
   [reitit.coercion.schema]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  ["/items/:item_id/attachments/"
   {:get {:accept "application/json"
          :coercion reitit.coercion.schema/coercion
          :swagger {:produces ["application/json"]}
          :parameters {:path {:pool_id s/Uuid
                              :item_id s/Uuid}
                       :query {(s/optional-key :page) s/Int
                               (s/optional-key :size) s/Int}}
          :produces ["application/json"]
          :handler index-resources
          :responses {200 {:description "OK"
                           :body get-attachments-response}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}

    :post {:accept "application/json"
           :description (str "- Limitations: " (config-get :api :attachments :max-size-mb) " MB\n"
                             "- Allowed File types: " (str/join ", " (config-get :api :attachments :allowed-file-types)))
           :coercion reitit.coercion.schema/coercion
           :swagger {:produces ["application/json"]}
           :produces ["application/json"]
           :parameters {:path {:pool_id s/Uuid
                               :item_id s/Uuid}
                        :header {:x-filename s/Str}}
           :handler post-resource
           :responses {200 {:description "OK"
                            :body attachment-response}
                                ;:body s/Any}
                       400 {:description "Bad Request (Coercion error)"
                            :body s/Any}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}])
