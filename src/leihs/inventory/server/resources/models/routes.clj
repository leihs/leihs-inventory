(ns leihs.inventory.server.resources.models.routes
  (:require
   [cheshire.core :as json]
   [clojure.set]
   [clojure.spec.alpha :as sa]
   [leihs.inventory.server.resources.models.main :refer [create-model-handler
                                                         delete-model-handler
                                                         get-manufacturer-handler
                                                         get-models-compatible-handler
                                                         get-models-handler
                                                         update-model-handler]]
   [leihs.inventory.server.resources.models.model-by-pool-form-fetch :refer [create-model-handler-by-pool-form-fetch]]
   [leihs.inventory.server.resources.models.model-by-pool-form-create :refer [create-model-handler-by-pool-form]]
   [leihs.inventory.server.resources.models.model-by-pool-form-update :refer [update-model-handler-by-pool-form]]
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

;(sa/def ::file multipart/temp-file-part)
;(sa/def ::name (sa/and string? #(not (clojure.string/blank? %))))
;(sa/def ::product (sa/and string? #(not (clojure.string/blank? %))))
;(sa/def ::version (sa/and string? #(not (clojure.string/blank? %))))
;(sa/def ::manufacturer (sa/and string? #(not (clojure.string/blank? %))))
;(sa/def ::isPackage (sa/and string? #(not (clojure.string/blank? %))))
;(sa/def ::description (sa/and string? #(not (clojure.string/blank? %))))
;(sa/def ::technicalDetails (sa/and string? #(not (clojure.string/blank? %))))
;(sa/def ::internalDescription (sa/and string? #(not (clojure.string/blank? %))))
;(sa/def ::importantNotes (sa/and string? #(not (clojure.string/blank? %))))
;(sa/def ::compatible_ids (sa/and string? #(not (clojure.string/blank? %))))
;(sa/def ::allocations (sa/and string? #(not (clojure.string/blank? %))))
;(sa/def ::category_ids (sa/and string? #(not (clojure.string/blank? %))))

;(require '[clojure.spec.alpha :as s])

;(require '[clojure.spec.alpha :as s])

(sa/def ::file multipart/temp-file-part)
(sa/def ::name (sa/nilable string?))
(sa/def ::product (sa/nilable string?))
(sa/def ::version (sa/nilable string?))
(sa/def ::manufacturer (sa/nilable string?))

;(sa/def ::isPackage (sa/nilable boolean?))                  ;; this causes errors OR blocks init-request in api
(sa/def ::isPackage (sa/nilable string?))


(sa/def ::description (sa/nilable string?))
(sa/def ::technicalDetails (sa/nilable string?))
(sa/def ::internalDescription (sa/nilable string?))
(sa/def ::importantNotes (sa/nilable string?))
(sa/def ::allocations (sa/nilable string?))
;(sa/def ::compatible_ids (sa/nilable string?))

(sa/def ::compatible_ids (sa/or
                          :multiple (sa/or :coll (sa/coll-of uuid?)
                                           :str string?)
                          :single uuid?
                          :none nil?))

;(sa/def ::category_ids (sa/nilable string?))

;(sa/def ::compatible_ids (sa/nilable (sa/or
;                                  :multiple (sa/coll-of uuid? :kind vector?)
;                                    :single uuid?
;                                    )))
;(sa/def ::category_ids (sa/nilable (sa/or :single uuid?
;                                  :multiple (sa/coll-of uuid? :kind vector?))))

;(sa/def ::category_ids (sa/nilable (sa/or
;                                  :multiple (sa/coll-of uuid? :kind vector?)
;                                   :single uuid?
;                                   )))

;;; TODO: initial validation-error
;(sa/def ::category_ids (sa/nilable (sa/or
;                                  :multiple (sa/coll-of uuid? :kind vector?)
;                                   :single uuid?
;                                   )))
;
;;; TODO: initial validation-error
;(sa/def ::category_ids  (sa/or
;                                  :multiple (sa/coll-of uuid? :kind vector?)
;                                   :single uuid?
;                                   :none nil?
;                                   ))

(defn comma-separated-uuids? [s]
  (and (string? s)
       (every? uuid? (map #(try (java.util.UUID/fromString %) (catch Exception _ nil))
                          (clojure.string/split s #",")))))

;; TODO: initial validation-error
(sa/def ::category_ids (sa/or
                        :multiple (sa/or :coll (sa/coll-of uuid?)
                                         :str string?)
                                    ;:str comma-separated-uuids?)
                        :single uuid?
                        :none nil?))

;(sa/def ::categories map?)
(sa/def ::categories string?)
(sa/def ::compatibles string?)

(sa/def ::images (sa/or :multiple (sa/coll-of ::file :kind vector?)
                        :single ::file))
(sa/def ::attachments (sa/or :multiple (sa/coll-of ::file :kind vector?)
                             :single ::file))

(sa/def ::entitlement_group_id uuid?)
(sa/def ::entitlement_id uuid?)
(sa/def ::quantity int?)
(sa/def ::entitlement (sa/keys :req-un [::entitlement_group_id ::entitlement_id ::quantity]))
(sa/def ::entitlements (sa/or
                        :multiple (sa/or :coll (sa/coll-of ::entitlement)
                                         :str string?)
                        :single ::entitlement
                        :none nil?))

(sa/def ::name string?)
(sa/def ::id uuid?)
(sa/def ::id-or-nil (sa/nilable uuid? ))
(sa/def ::inventory_bool boolean?)
(sa/def ::accessory (sa/keys :req-opt [::id-or-nil] :req-un [::name ::inventory_bool]))
(sa/def ::accessories (sa/or
                       :multiple (sa/or :coll (sa/coll-of ::accessory)
                                        :str string?)
                       :single ::accessory
                       :none nil?))

(sa/def ::key string?)
(sa/def ::value string?)
(sa/def ::property (sa/keys :req-opt [::id-or-nil] :req-un [::key ::value]))
(sa/def ::properties (sa/or
                      :multiple (sa/or :coll (sa/coll-of ::property)
                                       :str string?)
                      :single ::property
                      :none nil?))

;;; Spec for a single UUID string or UUID object
;(sa/def ::uuid (sa/or :uuid-instance uuid? :uuid-string #(re-matches #"[0-9a-fA-F\-]{36}" %)))
;
;;; Spec for a single partition entry
;(sa/def ::partition-entry
;  (sa/keys :req-un [::id ::group_id ::quantity]))
;
;;; Spec for `id`, `group_id`, and `quantity` fields in each partition entry
;(sa/def ::id ::uuid)
;(sa/def ::group_id ::uuid)
;(sa/def ::quantity pos-int?)
;
;;; Spec for the entire structure of `partitions_attributes`
;;(sa/def ::partitions-attributes
;;  (sa/map-of ::uuid ::partition-entry))
;
;(sa/def ::partitions-attributes
;  (sa/nilable (sa/map-of ::uuid ::partition-entry)))

(sa/def ::multipart (sa/keys :req-un [::product]
                             :opt-un [::version
                                      ::manufacturer
                                      ::isPackage
                                      ::description
                                      ::technicalDetails
                                      ::internalDescription
                                      ::importantNotes
                                  ;::allocations

                                      ;::category_ids
                                      ::categories

                                      ;::compatible_ids
                                      ::compatibles
                                      ::images
                                      ::attachments

                                  ;::partitions-attributes
                                      ::entitlements
                                      ::properties
                                      ::accessories]))

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

;(defn parse-json-string [v]
;  (if (string? v)
;    (try
;      (json/parse-string v true)
;      (catch Exception _
;        v))
;    v))
;
;(defn json-string-transformer [schema]
;  (fn [value]
;    (println ">o> json-string-transformer.v" value)
;    (if (map? value)
;      (reduce-kv (fn [m k v]
;                   (assoc m k (parse-json-string v)))
;        {}
;        value)
;      (parse-json-string value))))
;
;(def custom-coercion
;  (spec/create
;    {:transformers
;     {:body {:default json-string-transformer}}}))

(defn get-model-by-pool-route []
  ["/:pool_id"

   {:swagger {:conflicting true
              :tags ["Models by pool"] :security []}}

   ["/model"
    [""
     {:post {:accept "application/json"
             :swagger {:consumes ["multipart/form-data"]
                       :produces "application/json"}
             :summary "(DEV) | Form-Handler: Save data of 'Create model by form'"
             :description (str
                           " - Upload images and attachments \n"
                           " - Save data \n"
                           " - images: additional handling needed to process no/one/multiple files \n"
                           " - Browser creates thumbnails and attaches them as '*_thumb' \n\n\n"
                           " IMPORTANT\n - Upload of images with thumbnail (*_thumb) only")

             :coercion spec/coercion
             ;:coercion custom-coercion  ; Use the custom coercion here

             :parameters {:path {:pool_id uuid?}
                          :multipart ::multipart
                          ;:multipart ::model
                          }

             :handler create-model-handler-by-pool-form

             :responses {200 {:description "OK"}
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}}



     ]


    ["/:model_id"
     [""


      {:get {:accept "application/json"
             ;:swagger {:consumes ["multipart/form-data"]
             ;          :produces "application/json"}
             :summary "(DEV) | Form-Handler: Fetch form data"
             ;:description (str
             ;              " - Upload images and attachments \n"
             ;              " - Save data \n"
             ;              " - images: additional handling needed to process no/one/multiple files \n"
             ;              " - Browser creates thumbnails and attaches them as '*_thumb' \n\n\n"
             ;              " IMPORTANT\n - Upload of images with thumbnail (*_thumb) only")

             :coercion spec/coercion
             ;:coercion custom-coercion  ; Use the custom coercion here

             :parameters {:path {
                                 :pool_id uuid?
                                 :model_id uuid?
                                 }
                          ;:multipart ::multipart
                          ;:multipart ::model
                          }

             :handler create-model-handler-by-pool-form-fetch

             :responses {200 {:description "OK"}
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}


      :put {:accept "application/json"
             :swagger {:consumes ["multipart/form-data"]
                       :produces "application/json"}
             ;:summary "(DEV) | Form-Handler: Save data of 'Create model by form'"
             ;:description (str
             ;              " - Upload images and attachments \n"
             ;              " - Save data \n"
             ;              " - images: additional handling needed to process no/one/multiple files \n"
             ;              " - Browser creates thumbnails and attaches them as '*_thumb' \n\n\n"
             ;              " IMPORTANT\n - Upload of images with thumbnail (*_thumb) only")

             :coercion spec/coercion
             ;:coercion custom-coercion  ; Use the custom coercion here

             :parameters {:path {
                                 :pool_id uuid?
                                 :model_id uuid?
                                 }
                          :multipart ::multipart
                          ;:multipart ::model
                          }

             :handler update-model-handler-by-pool-form

             :responses {200 {:description "OK"}
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}}

      ]]

    ]




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
