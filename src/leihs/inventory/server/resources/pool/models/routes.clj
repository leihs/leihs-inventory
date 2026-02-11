(ns leihs.inventory.server.resources.pool.models.routes
  (:require
   [leihs.inventory.server.resources.pool.models.main :as models]
   [leihs.inventory.server.resources.pool.models.types :refer [description-model-form
                                                               get-compatible-response
                                                               post-response]]
   [reitit.coercion.schema]
   [reitit.coercion.spec :as spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  ["/models/"
   {:get {:conflicting true
          :accept "application/json"
          :produces ["application/json"]
          :coercion reitit.coercion.schema/coercion
          :swagger {:produces ["application/json"]}
          :summary "Used primarily to search for models as well as software"
          :description (str "- compatible-models: type model and software"
                            "\n"
                            "- autocomplete-search for item: type model")
          :parameters {:path {:pool_id s/Uuid}
                       :query {(s/optional-key :page) s/Int
                               (s/optional-key :size) s/Int
                               (s/optional-key :search) s/Str
                               (s/optional-key :search_term) s/Str
                               (s/optional-key :type) (s/enum "item" "model" "package")}}
          :handler models/index-resources
          :responses {200 {:description "OK"
                           :body get-compatible-response}
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}

    :post {:accept "application/json"
           :description description-model-form
           :coercion spec/coercion
           :parameters {:path {:pool_id uuid?}
                        :body :model/multipart}
           :handler models/post-resource
           :responses {200 {:description "OK"
                            :body post-response}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}])
