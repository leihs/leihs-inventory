(ns leihs.inventory.server.resources.models.routes
  (:require
   [clojure.set]
   [clojure.spec.alpha :as sa]
   [leihs.inventory.server.resources.models.main :refer [create-model-handler
                                                         delete-model-handler
                                                         get-manufacturer-handler
                                                         get-models-compatible-handler
                                                         get-models-handler
                                                         update-model-handler]]
   [leihs.inventory.server.resources.models.models-by-pool :refer [get-models-of-pool-handler
                                                                   create-model-handler-by-pool
                                                                   delete-model-handler-by-pool
                                                                   get-models-of-pool-auto-pagination-handler
                                                                   get-models-of-pool-handler
                                                                   get-models-of-pool-with-pagination-handler
                                                                   get-models-of-pool-auto-pagination-handler
                                                                   update-model-handler-by-pool]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec :as spec]
   [reitit.ring.middleware.multipart :as multipart]
   [ring.middleware.accept]
   [ring.util.response :as response]
   [schema.core :as s]
   [spec-tools.core :as st]))

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

(defn get-model-route []
  ["/"
   {:swagger {:conflicting true
              :tags ["Models"]
              :security []}}

   ["manufacturers"
    {:get {:conflicting true
           :accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :middleware [accept-json-middleware]
           :swagger {:produces ["application/json"]}
           :handler get-manufacturer-handler
           :responses {200 {:description "OK"
                            :body [s/Any]}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]

   ["models-compatibles"
    {:get {:conflicting true
           :accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :middleware [accept-json-middleware]
           :swagger {:produces ["application/json"]}

           :parameters {:query {(s/optional-key :page) s/Int
                                (s/optional-key :size) s/Int
                                ;(s/optional-key :sort_by) (s/enum :manufacturer-asc :manufacturer-desc :product-asc :product-desc)
                                ;(s/optional-key :filter_manufacturer) s/Str
                                ;(s/optional-key :filter_product) s/Str
                                }}
           :handler get-models-compatible-handler
           :responses {200 {:description "OK"
                            :body s/Any}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]

   ["models-compatibles/:model_id"
    {:get {:conflicting true
           :accept "application/json"
           :coercion reitit.coercion.schema/coercion
           :middleware [accept-json-middleware]
           :swagger {:produces ["application/json"]}
           :parameters {:path {:model_id s/Uuid}}
           :handler get-models-compatible-handler
           :responses {200 {:description "OK"
                            :body s/Any}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]

   ["models"
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
                             :body s/Any}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}

      :post {:summary "Create model."
             :accept "application/json"
             :coercion reitit.coercion.schema/coercion
             :parameters {:body {:product s/Str
                                 :version s/Str
                                 (s/optional-key :type) (s/enum "Software" "Model")
                                 (s/optional-key :is_package) s/Bool}}
             :middleware [accept-json-middleware]
             :handler create-model-handler
             :responses {200 {:description "Returns the created model."
                              :body s/Any}
                         400 {:description "Bad Request / Duplicate key value of ?product?"
                              :body s/Any}}}}]

    ["/:model_id"

     [""

      {:get {:accept "application/json"
             :conflicting true
             :coercion reitit.coercion.schema/coercion
             :middleware [accept-json-middleware]
             :swagger {:produces ["application/json"]}
             :handler get-models-handler
             :parameters {:path {:model_id s/Uuid}}
             :responses {200 {:description "OK"
                              :body s/Any}
                         204 {:description "No Content"}
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}

       :put {:accept "application/json"
             :coercion reitit.coercion.schema/coercion
             :parameters {:path {:model_id s/Uuid}
                          :body schema-min}
             :middleware [accept-json-middleware]
             :handler update-model-handler
             :responses {200 {:description "Returns the updated model."
                              :body s/Any}}}

       :delete {:accept "application/json"
                :coercion reitit.coercion.schema/coercion
                :parameters {:path {:model_id s/Uuid}}
                :middleware [accept-json-middleware]
                :handler delete-model-handler
                :responses {200 {:description "Returns the deleted model."
                                 :body s/Any}
                            400 {:description "Bad Request"
                                 :body s/Any}}}}]

     ["/items"
      [""
       {:get {:accept "application/json"
              :coercion reitit.coercion.schema/coercion
              :middleware [accept-json-middleware]
              :swagger {:produces ["application/json"]}
              :parameters {:path {:model_id s/Uuid}

                           :query {(s/optional-key :page) s/Int
                                   (s/optional-key :size) s/Int}}

              :handler get-models-of-pool-auto-pagination-handler
              :responses {200 {:description "OK"
                               :body s/Any}

                          404 {:description "Not Found"}
                          500 {:description "Internal Server Error"}}}}]

      ["/:item_id"
       {:get {:accept "application/json"
              :coercion reitit.coercion.schema/coercion
              :middleware [accept-json-middleware]
              :swagger {:produces ["application/json"]}
              :parameters {:path {:model_id s/Uuid
                                  :item_id s/Uuid}}
              :handler get-models-of-pool-handler
              :responses {200 {:description "OK"
                               ;:body (s/->Either [s/Any schema])}
                               :body s/Any}

                          404 {:description "Not Found"}
                          500 {:description "Internal Server Error"}}}}]]

     ["/properties"
      ["" {:get {:accept "application/json"
                 :coercion reitit.coercion.schema/coercion
                 :middleware [accept-json-middleware]
                 :swagger {:produces ["application/json"]}
                 :parameters {:path {:model_id s/Uuid}}
                 :handler get-models-of-pool-with-pagination-handler
                 :responses {200 {:description "OK"
                                  ;:body (s/->Either [s/Any schema])}
                                  :body s/Any}

                             404 {:description "Not Found"}
                             500 {:description "Internal Server Error"}}}}]

      ["/:properties_id"
       {:get {:accept "application/json"
              :coercion reitit.coercion.schema/coercion
              :middleware [accept-json-middleware]
              :swagger {:produces ["application/json"]}
              :parameters {:path {:model_id s/Uuid
                                  :properties_id s/Uuid}}
              :handler get-models-of-pool-handler

              :responses {200 {:description "OK"
                               ;:body (s/->Either [s/Any schema])}
                               :body s/Any}

                          404 {:description "Not Found"}
                          500 {:description "Internal Server Error"}}}}]]

     ["/accessories"
      ["" {:get {:accept "application/json"
                 :summary "(T)"
                 :coercion reitit.coercion.schema/coercion
                 :middleware [accept-json-middleware]
                 :swagger {:produces ["application/json"]}
                 :parameters {:path {:model_id s/Uuid}}
                 :handler get-models-of-pool-with-pagination-handler
                 :responses {200 {:description "OK"
                                  ;:body (s/->Either [s/Any schema])}
                                  :body s/Any}

                             404 {:description "Not Found"}
                             500 {:description "Internal Server Error"}}}}]

      ["/:accessories_id"
       {:get {:accept "application/json"
              :coercion reitit.coercion.schema/coercion
              :middleware [accept-json-middleware]
              :swagger {:produces ["application/json"]}
              :parameters {:path {:model_id s/Uuid
                                  :accessories_id s/Uuid}}
              :handler get-models-of-pool-handler

              :responses {200 {:description "OK"
                               ;:body (s/->Either [s/Any schema])}
                               :body s/Any}

                          404 {:description "Not Found"}
                          500 {:description "Internal Server Error"}}}}]]

     ["/attachments"
      ["" {:get {:accept "application/json"
                 :coercion reitit.coercion.schema/coercion
                 :middleware [accept-json-middleware]
                 :swagger {:produces ["application/json"]}
                 :parameters {:path {:model_id s/Uuid}}
                 :handler get-models-of-pool-with-pagination-handler
                 :responses {200 {:description "OK"
                                  ;:body (s/->Either [s/Any schema])}
                                  :body s/Any}

                             404 {:description "Not Found"}
                             500 {:description "Internal Server Error"}}}}]

      ["/:attachments_id"
       {:get {:accept "application/json"
              :coercion reitit.coercion.schema/coercion
              :middleware [accept-json-middleware]
              :swagger {:produces ["application/json"]}
              :parameters {:path {:model_id s/Uuid
                                  :attachments_id s/Uuid}}
              :handler get-models-of-pool-handler

              :responses {200 {:description "OK"
                               ;:body (s/->Either [s/Any schema])}
                               :body s/Any}

                          404 {:description "Not Found"}
                          500 {:description "Internal Server Error"}}}}]]

     ["/entitlements"
      ["" {:get {:accept "application/json"
                 :coercion reitit.coercion.schema/coercion
                 :middleware [accept-json-middleware]
                 :swagger {:produces ["application/json"]}
                 :parameters {:path {:model_id s/Uuid}}
                 :handler get-models-of-pool-with-pagination-handler
                 :responses {200 {:description "OK"
                                  ;:body (s/->Either [s/Any schema])}
                                  :body s/Any}

                             404 {:description "Not Found"}
                             500 {:description "Internal Server Error"}}}}]

      ["/:entitlement_id"
       {:get {:accept "application/json"
              :coercion reitit.coercion.schema/coercion
              :middleware [accept-json-middleware]
              :swagger {:produces ["application/json"]}
              :parameters {:path {:model_id s/Uuid
                                  :entitlement_id s/Uuid}}
              :handler get-models-of-pool-handler

              :responses {200 {:description "OK"
                               ;:body (s/->Either [s/Any schema])}
                               :body s/Any}

                          404 {:description "Not Found"}
                          500 {:description "Internal Server Error"}}}}]]

     ["/model-links"
      ["" {:get {:accept "application/json"
                 :coercion reitit.coercion.schema/coercion
                 :middleware [accept-json-middleware]
                 :swagger {:produces ["application/json"]}
                 :parameters {:path {:model_id s/Uuid}}
                 :handler get-models-of-pool-with-pagination-handler
                 :responses {200 {:description "OK"
                                  ;:body (s/->Either [s/Any schema])}
                                  :body s/Any}

                             404 {:description "Not Found"}
                             500 {:description "Internal Server Error"}}}}]

      ["/:model_link_id"
       {:get {:accept "application/json"
              :coercion reitit.coercion.schema/coercion
              :middleware [accept-json-middleware]
              :swagger {:produces ["application/json"]}
              :parameters {:path {:model_id s/Uuid
                                  :model_link_id s/Uuid}}
              :handler get-models-of-pool-handler

              :responses {200 {:description "OK"
                               ;:body (s/->Either [s/Any schema])}
                               :body s/Any}

                          404 {:description "Not Found"}
                          500 {:description "Internal Server Error"}}}}]]]]])

(sa/def ::file multipart/temp-file-part)
(sa/def ::name (sa/and string? #(not (clojure.string/blank? %))))
(sa/def ::product (sa/and string? #(not (clojure.string/blank? %))))
(sa/def ::version (sa/and string? #(not (clojure.string/blank? %))))
(sa/def ::manufacturer (sa/and string? #(not (clojure.string/blank? %))))
(sa/def ::isPackage (sa/and string? #(not (clojure.string/blank? %))))
(sa/def ::description (sa/and string? #(not (clojure.string/blank? %))))
(sa/def ::technicalDetails (sa/and string? #(not (clojure.string/blank? %))))
(sa/def ::internalDescription (sa/and string? #(not (clojure.string/blank? %))))
(sa/def ::importantNotes (sa/and string? #(not (clojure.string/blank? %))))
(sa/def ::compatibles (sa/and string? #(not (clojure.string/blank? %))))
(sa/def ::allocations (sa/and string? #(not (clojure.string/blank? %))))
(sa/def ::categories (sa/and string? #(not (clojure.string/blank? %))))

(sa/def ::images (sa/or :multiple (sa/coll-of ::file :kind vector?)
                        :single ::file))
(sa/def ::attachments (sa/or :multiple (sa/coll-of ::file :kind vector?)
                             :single ::file))

(sa/def ::multipart (sa/keys :req-un [::product]
                         :opt-un [::version
                                  ::manufacturer
                                  ::isPackage
                                  ::description
                                  ::technicalDetails
                                  ::internalDescription
                                  ::importantNotes
                                  ::compatibles
                                  ::allocations
                                  ::categories
                                  ::images
                                  ::attachments]))

(defn- process-attachments
  [request key]
  (let [files (get-in request [:parameters :multipart key])]
    (cond
      (nil? files) []
      (= files {}) []
      (= files [{}]) []

      (map? files)
      [{:filename (:filename files)
        :content-type (:content-type files)
        :size (:size files)}]

      (coll? files)
      (mapv (fn [file]
              {:filename (:filename file)
               :content-type (:content-type file)
               :size (:size file)})
        files)
      :else [])))

(defn get-model-by-pool-route []
  ["/:pool_id"

   {:swagger {:conflicting true
              :tags ["Models by pool"] :security []}}

   ["/model"
    [""
     {:post {
             ;:accept "multipart/form-data"
             :accept "application/json"
             :swagger {:consumes ["multipart/form-data"]
                       :produces "application/json"}
             :summary "(DEV) | Form-Handler: Save data of 'Create model by form'"
             :description (str
                           " - Upload images and attachments \n"
                           " - Save data \n"
                           " - images: additional handling needed to process no/one/multiple files \n"
                           " - Browser creates thumbnails and attaches them as '*_thumb' \n")
             :coercion spec/coercion
             :parameters {
                          :path {:pool_id uuid?}
                          :multipart ::multipart
                          ;:multipart ::model
                          }
             :handler (fn [request]

                        ;(response/response {:foo "bar" })

                        (let [params (get-in request [:parameters :multipart])
                              product (get-in request [:parameters :multipart :product])
                              file (-> request :parameters :multipart :file)
                              file-data {:filename (:filename file)
                                         :content-type (:content-type file)
                                         :size (:size file)}]

                          (response/response {:images (process-attachments request :images)
                                              :attachments (process-attachments request :attachments)
                                              :product product
                                              :data (-> request :parameters :multipart)
                                              :params-keys (keys params)}))
  )

             :responses {200 {:description "OK" }
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}}]]

   ["/models"
    [""
     {:get {:accept "application/json"
            :description "- https://staging.leihs.zhdk.ch/manage/8bd16d45-056d-5590-bc7f-12849f034351/models?search_term="
            :coercion reitit.coercion.schema/coercion
            :middleware [accept-json-middleware]
            :swagger {:produces ["application/json" "text/html"]}
            :parameters {:path {:pool_id s/Uuid}
                         :query {(s/optional-key :page) s/Int
                                 (s/optional-key :size) s/Int
                                 (s/optional-key :sort_by) (s/enum :manufacturer-asc :manufacturer-desc :product-asc :product-desc)
                                 (s/optional-key :filter_manufacturer) s/Str
                                 (s/optional-key :filter_product) s/Str}}

            ;:handler get-models-of-pool-handler
            :handler get-models-of-pool-with-pagination-handler

            :responses {200 {:description "OK"
                             :body (s/->Either [s/Any schema])}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}

      :post {:conflicting true
             :accept "application/json"
             :coercion reitit.coercion.schema/coercion
             :description "FYI: Use /model-group for category_id"
             :middleware [accept-json-middleware]
             :swagger {:produces ["application/json" "text/html"]}
             :parameters {:path {:pool_id s/Uuid}
                          :body {:product s/Str
                                 :category_ids [s/Uuid]
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
     ["" {:get {:accept "application/json"
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
                                    :body s/Any}}}}]

     ["/items"
      ["" {:get {:accept "application/json"
                 :coercion reitit.coercion.schema/coercion
                 :middleware [accept-json-middleware]
                 :swagger {:produces ["application/json"]}
                 :parameters {:path {:pool_id s/Uuid
                                     :model_id s/Uuid
                                 ;:item_id s/Uuid
                                     }}
                 :handler get-models-of-pool-with-pagination-handler
                 :responses {200 {:description "OK"
                              ;:body (s/->Either [s/Any schema])}
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
              :handler get-models-of-pool-with-pagination-handler
              :responses {200 {:description "OK"
                               ;:body (s/->Either [s/Any schema])}
                               :body s/Any}

                          404 {:description "Not Found"}
                          500 {:description "Internal Server Error"}}}}]]

     ["/properties"
      ["" {:get {:accept "application/json"
                 :coercion reitit.coercion.schema/coercion
                 :middleware [accept-json-middleware]
                 :swagger {:produces ["application/json"]}
                 :parameters {:path {:pool_id s/Uuid
                                     :model_id s/Uuid
                                     ;:item_id s/Uuid
                                     }}
                 :handler get-models-of-pool-with-pagination-handler
                 :responses {200 {:description "OK"
                                  ;:body (s/->Either [s/Any schema])}
                                  :body s/Any}

                             404 {:description "Not Found"}
                             500 {:description "Internal Server Error"}}}}]

      ["/:properties_id"
       {:get {:accept "application/json"
              :coercion reitit.coercion.schema/coercion
              :middleware [accept-json-middleware]
              :swagger {:produces ["application/json"]}
              :parameters {:path {:pool_id s/Uuid
                                  :model_id s/Uuid
                                  :properties_id s/Uuid}}
              :handler get-models-of-pool-with-pagination-handler

              :responses {200 {:description "OK"
                               ;:body (s/->Either [s/Any schema])}
                               :body s/Any}

                          404 {:description "Not Found"}
                          500 {:description "Internal Server Error"}}}}]]

     ["/accessories"
      ["" {:get {:accept "application/json"
                 :coercion reitit.coercion.schema/coercion
                 :middleware [accept-json-middleware]
                 :swagger {:produces ["application/json"]}
                 :parameters {:path {:pool_id s/Uuid
                                     :model_id s/Uuid
                                     ;:item_id s/Uuid
                                     }}
                 :handler get-models-of-pool-with-pagination-handler
                 :responses {200 {:description "OK"
                                  ;:body (s/->Either [s/Any schema])}
                                  :body s/Any}

                             404 {:description "Not Found"}
                             500 {:description "Internal Server Error"}}}}]

      ["/:accessories_id"
       {:get {:accept "application/json"
              :coercion reitit.coercion.schema/coercion
              :middleware [accept-json-middleware]
              :swagger {:produces ["application/json"]}
              :parameters {:path {:pool_id s/Uuid
                                  :model_id s/Uuid
                                  :accessories_id s/Uuid}}
              :handler get-models-of-pool-with-pagination-handler

              :responses {200 {:description "OK"
                               ;:body (s/->Either [s/Any schema])}
                               :body s/Any}

                          404 {:description "Not Found"}
                          500 {:description "Internal Server Error"}}}}]]

     ["/attachments"
      ["" {:get {:accept "application/json"
                 :coercion reitit.coercion.schema/coercion
                 :middleware [accept-json-middleware]
                 :swagger {:produces ["application/json"]}
                 :parameters {:path {:pool_id s/Uuid
                                     :model_id s/Uuid
                                     ;:item_id s/Uuid
                                     }}
                 :handler get-models-of-pool-with-pagination-handler
                 :responses {200 {:description "OK"
                                  ;:body (s/->Either [s/Any schema])}
                                  :body s/Any}

                             404 {:description "Not Found"}
                             500 {:description "Internal Server Error"}}}}]

      ["/:attachments_id"
       {:get {:accept "application/json"
              :coercion reitit.coercion.schema/coercion
              :middleware [accept-json-middleware]
              :swagger {:produces ["application/json"]}
              :parameters {:path {:pool_id s/Uuid
                                  :model_id s/Uuid
                                  :attachments_id s/Uuid}}
              :handler get-models-of-pool-with-pagination-handler

              :responses {200 {:description "OK"
                               ;:body (s/->Either [s/Any schema])}
                               :body s/Any}

                          404 {:description "Not Found"}
                          500 {:description "Internal Server Error"}}}}]]

     ["/entitlements"
      ["" {:get {:accept "application/json"
                 :summary "(T)"
                 :coercion reitit.coercion.schema/coercion
                 :middleware [accept-json-middleware]
                 :swagger {:produces ["application/json"]}
                 :parameters {:path {:pool_id s/Uuid
                                     :model_id s/Uuid
                                     ;:item_id s/Uuid
                                     }}
                 :handler get-models-of-pool-with-pagination-handler
                 :responses {200 {:description "OK"
                                  ;:body (s/->Either [s/Any schema])}
                                  :body s/Any}

                             404 {:description "Not Found"}
                             500 {:description "Internal Server Error"}}}}]

      ["/:entitlement_id"
       {:get {:accept "application/json"
              :coercion reitit.coercion.schema/coercion
              :middleware [accept-json-middleware]
              :swagger {:produces ["application/json"]}
              :parameters {:path {:pool_id s/Uuid
                                  :model_id s/Uuid
                                  :entitlement_id s/Uuid}}
              :handler get-models-of-pool-with-pagination-handler

              :responses {200 {:description "OK"
                               ;:body (s/->Either [s/Any schema])}
                               :body s/Any}

                          404 {:description "Not Found"}
                          500 {:description "Internal Server Error"}}}}]]

     ["/model-links"
      ["" {:get {:accept "application/json"
                 :coercion reitit.coercion.schema/coercion
                 :middleware [accept-json-middleware]
                 :swagger {:produces ["application/json"]}
                 :parameters {:path {:pool_id s/Uuid
                                     :model_id s/Uuid
                                     ;:item_id s/Uuid
                                     }}
                 :handler get-models-of-pool-with-pagination-handler
                 :responses {200 {:description "OK"
                                  ;:body (s/->Either [s/Any schema])}
                                  :body s/Any}

                             404 {:description "Not Found"}
                             500 {:description "Internal Server Error"}}}}]

      ["/:model_link_id"
       {:get {:accept "application/json"
              :coercion reitit.coercion.schema/coercion
              :middleware [accept-json-middleware]
              :swagger {:produces ["application/json"]}
              :parameters {:path {:pool_id s/Uuid
                                  :model_id s/Uuid
                                  :model_link_id s/Uuid}}
              :handler get-models-of-pool-with-pagination-handler

              :responses {200 {:description "OK"
                               ;:body (s/->Either [s/Any schema])}
                               :body s/Any}

                          404 {:description "Not Found"}
                          500 {:description "Internal Server Error"}}}}]]]]])
