(ns leihs.inventory.server.resources.models.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.models.main :as mn]
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
   (s/optional-key :manufacturer) (s/maybe s/Str)
   :product s/Str
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
   {:conflicting true
    :tags ["Models"]}

   [""
    {:get {:conflicting true
           :accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :middleware [accept-json-middleware]
           :swagger {:produces ["application/json" "text/html"]}
           :handler mn/get-models-handler
           :responses {200 {:description "OK"
                            :body (s/->Either [s/Any schema])}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}

     :post {:summary "Create model."
            :accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :parameters {:body schema-min}
            :middleware [accept-json-middleware]
            :handler mn/create-model-handler
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
           :handler mn/get-models-handler
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
           :handler mn/update-model-handler
           :responses {200 {:description "Returns the updated model."
                            :body s/Any}}}

     :delete {:accept "application/json"
              :coercion reitit.coercion.schema/coercion
              :parameters {:path {:id s/Uuid}}
              :middleware [accept-json-middleware]
              :handler mn/delete-model-handler
              :responses {200 {:description "Returns the deleted model."
                               :body s/Any}
                          400 {:description "Bad Request"
                               :body s/Any}}}}]])

(defn get-model-by-pool-route []
  ["/:pool_id"
   {:conflicting true
    :tags ["Models by pool"]}

   [""
    {:get {:conflicting true
           :accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :middleware [accept-json-middleware]
           :swagger {:produces ["application/json" "text/html"]}
           :parameters {:path {:pool_id s/Uuid}}
           :handler mn/get-models-of-pool-handler
           :responses {200 {:description "OK"
                            :body (s/->Either [s/Any schema])}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]

   ["/models/:model_id"
    {:get {:accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :middleware [accept-json-middleware]
           :swagger {:produces ["application/json" "text/html"]}
           :parameters {:path {:pool_id s/Uuid
                               :model_id s/Uuid}}
           :handler mn/get-models-of-pool-handler
           :responses {200 {:description "OK"
                            :body (s/->Either [s/Any schema])}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]])
