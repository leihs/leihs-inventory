(ns leihs.inventory.server.resources.models.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.models.by-pool :refer [get-models-of-pool-handler
                                                            create-model-handler-by-pool
                                                            get-models-of-pool-handler
                                                            update-model-handler-by-pool
                                                            delete-model-handler-by-pool]]
   [leihs.inventory.server.resources.models.main :refer [get-models-handler
                                                         create-model-handler
                                                         update-model-handler
                                                         delete-model-handler]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(def schema
  {:id s/Uuid
   :type s/Str
   (s/optional-key :manufacturer) (s/maybe s/Str)
   :product s/Str
   (s/optional-key :version) (s/maybe s/Str)
   (s/optional-key :info_url) (s/maybe s/Str)
   (s/optional-key :rental_price) (s/maybe s/Num)
   (s/optional-key :maintenance_period) (s/maybe s/Int)
   (s/optional-key :is_package) (s/maybe s/Bool)
   (s/optional-key :hand_over_note) (s/maybe s/Str)
   (s/optional-key :description) (s/maybe s/Str)
   (s/optional-key :internal_description) (s/maybe s/Str)
   (s/optional-key :technical_detail) (s/maybe s/Str)
   :created_at s/Inst
   :updated_at s/Inst
   (s/optional-key :cover_image_id) (s/maybe s/Uuid)})

(def schema-min
  {;:id s/Uuid
   :type s/Str
   :product s/Str
   (s/optional-key :manufacturer) (s/maybe s/Str)
    ;(s/optional-key :version) (s/maybe s/Str)
    ;(s/optional-key :info_url) (s/maybe s/Str)
    ;(s/optional-key :rental_price) (s/maybe s/Num)
    ;(s/optional-key :maintenance_period) (s/maybe s/Int)
    ;(s/optional-key :is_package) (s/maybe s/Bool)
    ;(s/optional-key :hand_over_note) (s/maybe s/Str)
    ;(s/optional-key :description) (s/maybe s/Str)
    ;(s/optional-key :internal_description) (s/maybe s/Str)
    ;(s/optional-key :technical_detail) (s/maybe s/Str)
    ;:created_at s/Inst
    ;:updated_at s/Inst
    ;(s/optional-key :cover_image_id) (s/maybe s/Uuid)
   })

(defn accept-json-middleware [handler]
  (fn [request]
    (let [accept-header (get-in request [:headers "accept"])]
      (if (and accept-header (re-matches #"^.*application/json.*$" accept-header))
        (handler request)
        rh/INDEX-HTML-RESPONSE-OK))))

(defn get-model-route []
  ["/models"

   {:swagger {:conflicting true
              :tags ["Models"] :security []}}

   [""
    {:get {:conflicting true
           :accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :middleware [accept-json-middleware]
           :swagger {:produces ["application/json" "text/html"]}
           :handler get-models-handler
           :description "Get all models, default: page=1, size=10, sort_by=manufacturer-asc"
           :parameters {:query {(s/optional-key :page) s/Int
                                (s/optional-key :size) s/Int
                                (s/optional-key :sort_by) (s/enum :manufacturer-asc :manufacturer-desc :product-asc :product-desc)
                                (s/optional-key :filter_manufacturer) s/Str
                                (s/optional-key :filter_product) s/Str}}
           :responses {200 {:description "OK"
                            :body (s/->Either [s/Any schema])}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}

     :post {:summary "Create model."
            :accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :parameters {:body schema-min}
            :middleware [accept-json-middleware]
            :handler create-model-handler
            :responses {200 {:description "Returns the created model."
                             :body s/Any}
                        400 {:description "Bad Request / Duplicate key value of ?product?"
                             :body s/Any}}}}]

   ["/:id"
    {:get {:accept "application/json"
           :conflicting true
           :coercion reitit.coercion.schema/coercion
           :middleware [accept-json-middleware]
           :swagger {:produces ["application/json"]}
           :handler get-models-handler
           :parameters {:path {:id s/Uuid}}
           :responses {200 {:description "OK"
                            :body (s/->Either [s/Any schema])}
                       204 {:description "No Content"}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}

     :put {:accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :parameters {:path {:id s/Uuid}
                        :body schema-min}
           :middleware [accept-json-middleware]
           :handler update-model-handler
           :responses {200 {:description "Returns the updated model."
                            :body s/Any}}}

     :delete {:accept "application/json"
              :coercion reitit.coercion.schema/coercion
              :parameters {:path {:id s/Uuid}}
              :middleware [accept-json-middleware]
              :handler delete-model-handler
              :responses {200 {:description "Returns the deleted model."
                               :body s/Any}
                          400 {:description "Bad Request"
                               :body s/Any}}}}]])

(defn get-model-by-pool-route []
  ["/:pool_id"

   {:swagger {:conflicting true
              :tags ["Models by pool"] :security []}}

   [""
    {:get {:conflicting true
           :accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :middleware [accept-json-middleware]
           :swagger {:produces ["application/json" "text/html"]}
           :parameters {:path {:pool_id s/Uuid}}
           :handler get-models-of-pool-handler
           :responses {200 {:description "OK"
                            :body (s/->Either [s/Any schema])}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]
   ["/models"
    [""
     {:post {:conflicting true
             :accept "application/json"
             :coercion reitit.coercion.schema/coercion
             :middleware [accept-json-middleware]
             :swagger {:produces ["application/json" "text/html"]}
             :parameters {:path {:pool_id s/Uuid}
                          :body {:product s/Str
                                 :version s/Str
                                 (s/optional-key :type) (s/enum "Software" "Model")
                                  ;;default: Model
                                 (s/optional-key :is_package) s/Bool}}

             :handler create-model-handler-by-pool
             :responses {200 {:description "OK"
                              :body s/Any}
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}}]

    ["/:model_id"
     {:get {:accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :middleware [accept-json-middleware]
            :swagger {:produces ["application/json" "text/html"]}
            :parameters {:path {:pool_id s/Uuid
                                :model_id s/Uuid}}
            :handler get-models-of-pool-handler
            :responses {200 {:description "OK"
                             :body (s/->Either [s/Any schema])}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}

      :put {:accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :parameters {:path {:pool_id s/Uuid :model_id s/Uuid}
                         :body schema-min}
            :middleware [accept-json-middleware]
            :handler update-model-handler-by-pool
            :responses {200 {:description "Returns the updated model."
                             :body s/Any}}}

      :delete {:accept "application/json"
               :coercion reitit.coercion.schema/coercion
               :parameters {:path
                            {:pool_id s/Uuid :model_id s/Uuid}}
               :middleware [accept-json-middleware]
               :handler delete-model-handler-by-pool
               :responses {200 {:description "Returns the deleted model."
                                :body s/Any}
                           400 {:description "Bad Request"
                                :body s/Any}}}}]]])
