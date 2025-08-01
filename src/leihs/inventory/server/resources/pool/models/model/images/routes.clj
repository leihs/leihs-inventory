(ns leihs.inventory.server.resources.pool.models.model.images.routes
  (:require
   [clojure.string :as str]
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.models.model.constants :refer [config-get]]
   [leihs.inventory.server.resources.pool.models.model.images.main :as images]
   [leihs.inventory.server.resources.pool.models.model.images.types :refer [get-images-response
                                                                            post-response]]
   [leihs.inventory.server.utils.middleware :refer [accept-json-middleware]]
   [reitit.coercion.schema]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  ["/models/:model_id/images/"
   {:post {:accept "application/json"
           :summary (fe "")
           :description (str "- Limitations: " (config-get :api :images :max-size-mb) " MB\n"
                             "- Allowed File types: " (str/join ", " (config-get :api :images :allowed-file-types)) "\n"
                             "- Creates automatically a thumbnail (" (config-get :api :images :thumbnail :width-px)
                             "px x " (config-get :api :images :thumbnail :height-px) "px)\n")
           :swagger {:consumes ["application/json"]
                     :produces "application/json"}
           :coercion reitit.coercion.schema/coercion
           :middleware [accept-json-middleware]
           :parameters {:path {:pool_id s/Uuid
                               :model_id s/Uuid}
                        :header {:x-filename s/Str}}
           :handler images/post-resource
           :responses {200 {:description "OK" :body post-response}
                       404 {:description "Not Found"}
                       411 {:description "Length Required"}
                       413 {:description "Payload Too Large"}
                       500 {:description "Internal Server Error"}}}

    :get {:accept "application/json"
          :coercion reitit.coercion.schema/coercion
          :middleware [accept-json-middleware]
          :swagger {:produces ["application/json"]}
          :parameters {:path {:pool_id s/Uuid
                              :model_id s/Uuid}
                       :query {(s/optional-key :page) s/Int
                               (s/optional-key :size) s/Int}}
          :handler images/index-resources
          :responses {200 {:description "OK"
                           :body get-images-response}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}}])
