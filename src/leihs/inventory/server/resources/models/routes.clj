(ns leihs.inventory.server.resources.models.routes
  (:require

   [leihs.inventory.server.resources.models.tree.tree  :refer [tree]]


   [leihs.inventory.server.resources.models.tree.filter :as filter]
   [leihs.core.core :refer [presence]]


   [clojure.spec.alpha :as sa]
   [leihs.inventory.server.resources.models.form.items.model-by-pool-form-create :refer [create-items-handler-by-pool-form]]

   [leihs.inventory.server.resources.models.form.items.model-by-pool-form-fetch :refer [fetch-items-handler-by-pool-form]]
   [leihs.inventory.server.resources.models.form.items.model-by-pool-form-update :refer [update-items-handler-by-pool-form]]
   [leihs.inventory.server.resources.models.form.license.model-by-pool-form-create :refer [create-license-handler-by-pool-form]]

   [leihs.inventory.server.resources.models.form.license.model-by-pool-form-fetch :refer [fetch-license-handler-by-pool-form-fetch]]
   [leihs.inventory.server.resources.models.form.license.model-by-pool-form-update :refer [update-license-handler-by-pool-form]]
   [leihs.inventory.server.resources.models.form.model.model-by-pool-form-create :refer [create-model-handler-by-pool-form]]

   [leihs.inventory.server.resources.models.form.model.model-by-pool-form-fetch :refer [create-model-handler-by-pool-form-fetch]]
   [leihs.inventory.server.resources.models.form.model.model-by-pool-form-update :refer [update-model-handler-by-pool-form]]
   [leihs.inventory.server.resources.models.form.option.model-by-pool-form-create :refer [create-option-handler-by-pool-form]]

   [leihs.inventory.server.resources.models.form.option.model-by-pool-form-fetch :refer [fetch-option-handler-by-pool-form]]
   [leihs.inventory.server.resources.models.form.option.model-by-pool-form-update :refer [update-option-handler-by-pool-form]]
   [leihs.inventory.server.resources.models.form.package.model-by-pool-form-create :refer [create-package-handler-by-pool-form]]

   [leihs.inventory.server.resources.models.form.package.model-by-pool-form-fetch :refer [fetch-package-handler-by-pool-form]]
   [leihs.inventory.server.resources.models.form.package.model-by-pool-form-update :refer [update-package-handler-by-pool-form]]
   [leihs.inventory.server.resources.models.form.software.model-by-pool-form-create :refer [create-software-handler-by-pool-form]]

   [leihs.inventory.server.resources.models.form.software.model-by-pool-form-fetch :refer [create-software-handler-by-pool-form-fetch]]
   [leihs.inventory.server.resources.models.form.software.model-by-pool-form-update :refer [update-software-handler-by-pool-form]]
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
   [leihs.inventory.server.utils.auth.role-auth :refer [permission-by-role-and-pool]]

   [leihs.inventory.server.utils.auth.roles :as roles]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec :as spec]
   [reitit.ring.middleware.multipart :as multipart]
   [ring.middleware.accept]
   [ring.util.response :as response]
   [schema.core :as s]
   [spec-tools.core :as st]
   [spec-tools.data-spec :as ds]))

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
  {:type s/Str
   :product s/Str
   (s/optional-key :manufacturer) (s/maybe s/Str)})

(defn get-model-route []
  ["/"
   {:swagger {:conflicting true
              :tags ["Models"]
              :security []}}

   ["manufacturers"
    {:get {:conflicting true
           :summary "Get manufacturers [v0]"
           :accept "application/json"
           :description "'search-term' starts working with at least one character, considers:\n
- manufacturer\n
- product\n\n
HINT: 'in-detail'-option works for models with set 'search-term' only\n"
           :coercion reitit.coercion.schema/coercion
           :middleware [accept-json-middleware]
           :swagger {:produces ["application/json"]}
           :handler get-manufacturer-handler
           :parameters {:query {(s/optional-key :type) (s/enum "Software" "Model")
                                (s/optional-key :search-term) s/Str
                                (s/optional-key :in-detail) (s/enum "true" "false")}}
           :responses {200 {:description "OK"
                            :body [(s/conditional
                                    map? {:id s/Uuid
                                          :manufacturer s/Str
                                          :product s/Str
                                          :version (s/maybe s/Str)
                                          :model_id s/Uuid}
                                    string? s/Str)]}
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
(sa/def ::name (sa/nilable string?))
(sa/def ::product (sa/nilable string?))
(sa/def ::item_version (sa/nilable string?))
(sa/def ::version (sa/nilable string?))
(sa/def ::manufacturer (sa/nilable string?))
(sa/def ::isPackage (sa/nilable string?))
(sa/def ::description (sa/nilable string?))
(sa/def ::technicalDetails (sa/nilable string?))
(sa/def ::internalDescription (sa/nilable string?))
(sa/def ::importantNotes (sa/nilable string?))
(sa/def ::allocations (sa/nilable string?))

(sa/def ::compatible_ids (sa/or
                          :multiple (sa/or :coll (sa/coll-of uuid?)
                                           :str string?)
                          :single uuid?
                          :none nil?))

;; TODO: initial validation-error
(sa/def ::category_ids (sa/or
                        :multiple (sa/or :coll (sa/coll-of uuid?)
                                         :str string?)
                        :single uuid?
                        :none nil?))

(sa/def ::name string?)
(sa/def ::delete boolean?)
(sa/def ::position int?)
(sa/def ::id uuid?)
(sa/def ::id-or-nil (sa/nilable uuid?))
(sa/def ::name string?)
(sa/def ::created_at any?)
(sa/def ::updated_at any?)

(sa/def ::type
  (sa/and string? #{"Category"}))

(sa/def ::category (sa/keys :opt-un [::delete ::created_at ::updated_at]
                            :req-un [::id ::type ::name]))
(sa/def ::categories (sa/or
                      :single (sa/or :coll (sa/coll-of ::category)
                                     :str string?)
                      :none nil?))

(sa/def ::compatible (sa/keys :opt-un [::delete]
                              :req-un [::id ::product]))
(sa/def ::compatibles (sa/or
                       :single (sa/or :coll (sa/coll-of ::compatible)
                                      :str string?)
                       :none nil?))
(sa/def ::images-to-delete string?)
(sa/def ::attachments-to-delete string?)

(sa/def ::images (sa/or :multiple (sa/coll-of ::file :kind vector?)
                        :single ::file))
(sa/def ::attachments any?)
(sa/def ::entitlement_group_id uuid?)
(sa/def ::entitlement_id uuid?)
(sa/def ::quantity int?)
(sa/def ::entitlement (sa/keys :opt-un [::name ::delete ::position]
                               :req-un [::entitlement_group_id ::entitlement_id ::quantity]))
(sa/def ::entitlements (sa/or
                        :single (sa/or :coll (sa/coll-of ::entitlement)
                                       :str string?)
                        :none nil?))
(sa/def ::inventory_bool boolean?)
(sa/def ::accessory (sa/keys :req-opt [::id-or-nil ::delete] :req-un [::name ::inventory_bool]))
(sa/def ::accessories (sa/or
                       :single (sa/or :coll (sa/coll-of ::accessory)
                                      :str string?)
                       :none nil?))
(sa/def ::properties string?)
(sa/def ::serial_number string?)
(sa/def ::note string?)
(sa/def ::status_note string?)

(sa/def ::owner_id uuid?)

(sa/def ::building_id uuid?)
(sa/def ::room_id uuid?)
(sa/def ::software_id uuid?)
(sa/def ::supplier_id (sa/nilable string?))
(sa/def ::model_id uuid?)

(sa/def ::inventory_code string?)
(sa/def ::item_version string?)
(sa/def ::is_incomplete boolean?)
(sa/def ::is_broken boolean?)
(sa/def ::retired boolean?)
(sa/def ::retired_reason string?)
(sa/def ::price string?)
(sa/def ::invoice_date string?)
(sa/def :lr/invoice_date any?)
(sa/def ::invoice_number string?)

(sa/def ::shelf string?) ;; FIXME

(sa/def ::user_name string?)
(sa/def ::activation_type string?)
(sa/def ::dongle_id string?)
(sa/def ::license_type string?)

(sa/def ::total_quantity string?)

(sa/def ::operating_system string?)
(sa/def ::quantity_allocations any?)
(sa/def ::maintenance_currency string?)

(sa/def ::license_expiration string?)
(sa/def ::installation string?)
(sa/def ::p4u string?)
(sa/def ::reference string?)
(sa/def ::project_number string?)
(sa/def ::procured_by string?)
(sa/def ::maintenance_contract string?)
(sa/def ::maintenance_expiration string?)
(sa/def ::maintenance_price string?)

(sa/def ::key string?)
(sa/def ::value string?)
(sa/def :simple/properties string?)
(sa/def ::property (sa/keys :req-opt [::id-or-nil] :req-un [::key ::value]))

;; deprecated / not in use?
(sa/def :license/properties (sa/keys :req-opt [::activation_type
                                               ::dongle_id
                                               ::license_type
                                               ::total_quantity
                                               ::license_expiration
                                               ::p4u
                                               ::reference
                                               ::project_number
                                               ::procured_by
                                               ::maintenance_contract
                                               ::maintenance_expiration
                                               ::maintenance_price] :req-un []))

(sa/def :license/multipart (sa/keys :opt-un [::model_id
                                             ::supplier_id
                                             ::attachments-to-delete
                                             ::attachments
                                             ::retired_reason
                                             :simple/properties
                                             ::owner_id
                                             ::item_version]
                                    :req-un [::serial_number
                                             ::note
                                             ::invoice_date
                                             ::price
                                             ::retired
                                             ::is_borrowable
                                             ::inventory_code]))

(sa/def :item/multipart (sa/keys :opt-un [::model_id
                                          ::supplier_id
                                          ::attachments-to-delete
                                          ::attachments
                                          ::retired_reason
                                             ;:simple/properties
                                          ::owner_id
                                          ::user_name
                                             ;::item_version
                                          ]
                                 :req-un [::serial_number
                                          ::note
                                          ::invoice_date
                                          ::invoice_number
                                          ::price
                                          ::shelf
                                          ::inventory_code
                                          ::retired

                                          ::is_borrowable
                                          ::is_broken
                                          ::is_incomplete

                                             ;::building_id
                                          ::room_id

                                          ::status_note
                                             ;::supplier_id
                                             ;::owner_id
                                          ::properties]))

(sa/def :package/multipart (sa/keys :opt-un [::model_id
                                             ::supplier_id
                                             ::attachments-to-delete
                                             ::attachments
                                             ::retired_reason
                                             ;:simple/properties
                                             ::owner_id
                                             ::user_name
                                             ;::item_version
                                             ]
                                    :req-un [;::serial_number
                                             ::note
                                             ;::invoice_date
                                             ;::invoice_number
                                             ::price
                                             ::shelf
                                             ::inventory_code
                                             ::retired

                                             ::is_borrowable
                                             ::is_broken
                                             ::is_incomplete

                                             ;::building_id
                                             ::room_id

                                             ::status_note
                                             ;::supplier_id
                                             ;::owner_id
                                             ::properties]))

(sa/def ::inventory_code string?)
(sa/def ::inventory_pool_id uuid?)
(sa/def ::responsible any?)
(sa/def :nil/responsible (sa/nilable any?))
(sa/def :nil/invoice_number (sa/nilable any?))
(sa/def :nil/note (sa/nilable string?))
(sa/def :nil/serial_number (sa/nilable string?))

(sa/def ::responsible_department uuid?)

(sa/def ::data-spec
  (st/spec {:spec (sa/keys :req-un [::inventory_code
                                    ::inventory_pool_id
                                    ::responsible_department])
            :description "Data section of the body"}))

(defn nil-or [pred]
  (sa/or :nil nil? :value pred))

(sa/def ::active boolean?)
(sa/def ::data any?)
(sa/def ::group string?)
(sa/def ::id string?)
(sa/def ::label string?)
;(sa/def ::owner (nil-or uuid?))
(sa/def ::owner (nil-or string?))
(sa/def ::position int?)
(sa/def ::role (nil-or string?))
(sa/def ::role_default string?)
(sa/def ::target (nil-or string?))
(sa/def ::target_default string?)

(sa/def ::fields-spec
  (st/spec {:spec (sa/keys :req-un [::active
                                    ::data
                                    ::id
                                    ::label
                                    ::owner
                                    ::position
                                    ::role
                                    ::role_default
                                    ::target_default]
                           :opt-un [::group ::target])
            :description "Fields section of the body"}))

(sa/def :get-package-response/body-spec
  (st/spec {:spec (sa/keys :req-un [::data-spec ::fields-spec])
            :description "Body of the request"}))

;; Define primitive field specs
(sa/def ::note string?)
(sa/def ::is_inventory_relevant boolean?)
;(sa/def ::last_check inst?) ;; Assuming it is a date
(sa/def ::last_check any?)
(sa/def ::user_name string?)

;(sa/def ::price int?)
(sa/def ::price string?)
(sa/def :lr/price any?)

(sa/def ::shelf string?)
(sa/def ::inventory_code string?)
(sa/def ::retired boolean?)
(sa/def ::retired_reason string?)
(sa/def ::is_broken boolean?)
(sa/def ::is_incomplete boolean?)
(sa/def ::is_borrowable boolean?)
(sa/def ::status_note string?)
(sa/def ::room_id uuid?)
(sa/def ::owner_id uuid?)

;; Define the schema for items in items_attributes
(sa/def ::item_inventory_code string?)
(sa/def ::item_id uuid?)

(sa/def ::items_attributes
  (sa/coll-of (sa/keys :req-un [::item_inventory_code ::item_id]) :kind vector?))

;; Define the main spec for the request body
(sa/def :package-put/inventory-attributes
  (st/spec {:spec (sa/keys :req-un [;::note
                                    ::is_inventory_relevant
                                    ::last_check
                                    ::user_name
                                    ::price
                                    ::shelf
                                    ::inventory_code
                                    ::retired
                                   ;::retired_reason
                                    ::is_broken
                                    ::is_incomplete
                                    ::is_borrowable
                                    ::status_note
                                    ::room_id
                                    ::model_id
                                    ::owner_id]

                           :opt-un [::note ::retired_reason
                                    ::items_attributes])

            :description "Inventory attributes with details"}))

;; Ensure all spec keys are properly namespaced
(sa/def :res/properties map?)
(sa/def :res/inventory_code string?)
(sa/def :res/owner_id uuid?)
(sa/def :res/is_borrowable boolean?)
(sa/def :res/retired inst?) ;; Date
(sa/def :res/is_inventory_relevant boolean?)
(sa/def :res/last_check inst?) ;; Date
(sa/def :res/shelf string?)
(sa/def :res/status_note string?)
(sa/def :res/name (sa/nilable string?))
(sa/def :res/invoice_number (sa/nilable string?))
(sa/def :res/is_broken boolean?)
(sa/def :res/note string?)
(sa/def :res/updated_at inst?) ;; Date
(sa/def :res/retired_reason string?)
(sa/def :res/responsible (sa/nilable string?))
(sa/def :res/invoice_date (sa/nilable inst?)) ;; Date
(sa/def :res/model_id uuid?)
(sa/def :res/supplier_id (sa/nilable uuid?))
(sa/def :res/parent_id (sa/nilable uuid?))
(sa/def :res/id uuid?)
(sa/def :res/inventory_pool_id uuid?)
(sa/def :res/is_incomplete boolean?)
(sa/def :res/item_version (sa/nilable string?))
(sa/def :res/needs_permission boolean?)
(sa/def :res/user_name string?)
(sa/def :res/room_id uuid?)
(sa/def :res/serial_number (sa/nilable string?))
;(sa/def :res/price double?)
(sa/def :res/price (sa/nilable any?))
(sa/def :res/created_at inst?) ;; Date
(sa/def :res/items_attributes any?) ;; Date
(sa/def :res/insurance_number (sa/nilable string?))


;; ✅ Correct: Define the `data` spec properly
(sa/def :res/data
  (st/spec {:spec (sa/keys :req-un [:res/inventory_code
                                    :nil/retired
                                    :res/is_borrowable
                                    :res/is_inventory_relevant
                                    :res/is_broken
                                    :res/is_incomplete
                                    :nil/last_check
                                    :nil/shelf
                                    :nil/status_note
                                    :nil/user_name
                                    :res/room_id
                                    :res/model_id
                                    :res/owner_id
                                    :res/price]

                           :opt-un [:res/note
                                    :res/items_attributes
                                    :res/name
                                    :res/invoice_number
                                    :res/properties
                                    :res/updated_at
                                    :nil/retired_reason
                                    :nil/note
                                    :res/responsible
                                    :res/invoice_date
                                    :res/supplier_id
                                    :res/parent_id
                                    :res/id
                                    :res/inventory_pool_id
                                    :res/item_version
                                    :res/needs_permission
                                    :res/serial_number
                                    :res/created_at
                                    :res/insurance_number
                                    :res/insurance_number])

            :description "Inventory item data"}))

(sa/def :res/validation (sa/coll-of map? :kind vector?))

;; Define the main coercion spec with properly namespace-qualified keys
(sa/def :package-put-response/inventory-item
  (st/spec {:spec (sa/keys :req-un [:res/data]
                           :opt-un [:res/validation :res/items_attributes])
            :description "Complete inventory response"}))

;; Define the main coercion spec with properly namespace-qualified keys
(sa/def :package-put-response2/inventory-item
  (st/spec {:spec (sa/keys :req-un [:res/data]
                           :opt-un [:res/validation])
            :description "Complete inventory response"}))

(sa/def :software/properties (sa/or
                              :single (sa/or :coll (sa/coll-of ::property)
                                             :str string?)
                              :none nil?))

(sa/def :option/multipart (sa/keys :req-un [::product]
                                   :opt-un [::version
                                            ::price
                                            ::inventory_code]))

(sa/def ::multipart (sa/keys :req-un [::product]
                             :opt-un [::version
                                      ::manufacturer
                                      ::isPackage
                                      ::description
                                      ::technicalDetails
                                      ::internalDescription
                                      ::importantNotes
                                      ::categories
                                      ::attachments-to-delete
                                      ::images-to-delete
                                      ::compatibles
                                      ::images
                                      ::attachments
                                      ::entitlements
                                      :software/properties
                                      ::accessories]))

(defn nil-or [pred]
  (sa/or :nil nil? :value pred))

(def response-option-object {:id uuid?
                             :inventory_pool_id uuid?
                             :inventory_code string?
                             :manufacturer any?
                             :product string?
                             :version string?
                             :price any?})

(def response-option [response-option-object])

(def FieldDataSchema ;; FIXME
  {:type string?
   :group string?
   :label string?
   (ds/opt :values) any?
   (ds/opt :default) any?
   :attribute any?
   (ds/opt :forPackage) any?
   (ds/opt :target_type) any?
   (ds/opt :values_label_method) any?
   (ds/opt :values_dependency_field_id) any?
   (ds/opt :required) any?
   :permissions {:role string?
                 :owner boolean?}})

(def FieldSchema
  {:role (sa/nilable string?)
   :group (sa/nilable string?)
   :group_default string?
   :role_default string?
   (ds/opt :target_default) string?
   :active boolean?
   :label string?
   :id string?
   :position int?
   (ds/opt :target) (sa/nilable string?)
   :owner (sa/nilable string?)

   ;:data FieldDataSchema                                    ;; FIXME broken
   :data any?})

(def DataSchema
  {:inventory_pool_id uuid?
   :responsible_department string?
   :quantity int?
   :inventory_code string?})

(def ResponseBodySchema
  {:data DataSchema
   :fields [FieldSchema]})

;; ----------------------

;(ns my-api.schema
;  (:require [clojure.spec.alpha :as s]))

;; Define a UUID type (as string)
(sa/def ::uuid string?)

;; Define a nullable string
(sa/def ::nullable-string (sa/nilable string?))

;; Define boolean and integer types
(sa/def ::boolean boolean?)
(sa/def ::integer int?)

;; Define "data" schema
;(sa/def ::inventory_pool_id ::uuid)
(sa/def ::inventory_pool_id string?)
(sa/def ::inventory_pool_id any?)
(sa/def ::responsible_department ::uuid)
(sa/def ::inventory_code string?)

(sa/def ::DataSchema
  (sa/keys :req-un [::inventory_pool_id
                    ::responsible_department
                    ::inventory_code]))

;; Define "permissions" schema inside "data"
(sa/def ::role string?)
(sa/def ::owner ::boolean)

(sa/def ::PermissionsSchema
  (sa/keys :req-un [::role ::owner]))

;; Define "data" inside "fields"
(sa/def ::type string?)
(sa/def ::group string?)
(sa/def ::label string?)
(sa/def ::attribute any?)
;(sa/def ::permissions ::PermissionsSchema)
(sa/def ::permissions any?)
(sa/def ::forPackage boolean?)

;; Define "fields" schema
(sa/def :nil/role (sa/nilable string?))
(sa/def :nil/group (sa/nilable string?))
(sa/def ::group_default string?)
(sa/def ::target_type string?)
(sa/def ::role_default string?)
(sa/def ::target_default string?)
(sa/def ::active ::boolean)
(sa/def ::label string?)
;(sa/def :bool/owner boolean?)
;(sa/def :bool/owner (sa/nilable boolean?))
(sa/def :bool/owner (sa/nilable string?))
(sa/def ::id string?)
(sa/def :lr/id uuid?)
(sa/def ::position ::integer)
(sa/def ::target ::nullable-string)
(sa/def ::owner ::nullable-string) ;; "true" is string, but could be coerced to boolean


(sa/def ::FieldDataSchema
  (sa/keys :req-un [::inventory_pool_id ::responsible_department ::inventory_code]))

(sa/def ::FieldSchema
  (sa/keys :req-un [:nil/role
                    :nil/group
                    ::group_default
                    ::role_default
                    ::target_default
                    ::active
                    ::label
                    :any/id
                    ::position
                    ::target
                    :bool/owner
                    ::data]))


;; ----------------------
(sa/def :nil/id (sa/nilable uuid?))
(sa/def :nil/updated_at (sa/nilable any?))
(sa/def :nil/created_at (sa/nilable any?))
(sa/def :nil/name (sa/nilable string?))
(sa/def :nil/status_note (sa/nilable string?))
(sa/def :nil/shelf (sa/nilable string?))
(sa/def :nil/last_check (sa/nilable string?))
(sa/def :nil/item_version (sa/nilable string?))
(sa/def :nil2/item_version (sa/nilable any?))
(sa/def :nil/retired (sa/nilable any?))
(sa/def :nil/retired_reason (sa/nilable string?))
(sa/def :nil/price (sa/nilable string?))
(sa/def :nil/invoice_date (sa/nilable string?))
(sa/def ::properties any?)
(sa/def :nil/parent_id (sa/nilable uuid?))
(sa/def :nil/insurance_number (sa/nilable any?))
(sa/def :nil/user_name (sa/nilable any?))
(sa/def :nil/supplier_id (sa/nilable any?))

(sa/def ::needs_permission boolean?)
(sa/def ::is_incomplete boolean?)

(def DataSchema2
  (sa/keys :req-un [::inventory_code
                    ::owner_id
                    ::is_borrowable
                    :nil/retired
                    ::is_inventory_relevant
                    ::last_check
                    ::shelf
                    ::status_note
                    :nil/name
                    ::invoice_number
                    ::is_broken
                    ::note

                    :nil/updated_at
                    :nil/retired_reason
                   ;::retired_reason
                   ;::responsible
                    :nil/responsible
                    :nil/invoice_date
                    ::model_id
                    ::supplier_id
                    :nil/parent_id
                    :nil/id
                    ::inventory_pool_id
                    ::is_incomplete
                   ;:nil/item_version
                    ::needs_permission
                    ::user_name
                    ::room_id
                    ::serial_number
                    :nil/price
                    :nil/created_at
                    :nil/insurance_number
                    ::properties]
           :opt-un [:nil2/item_version]))

(def test_ResponseBodySchema
  {:data DataSchema2
   :validation [any?]})

(sa/def ::description (sa/nilable string?))
(sa/def ::is_package boolean?)
(sa/def ::attachments (sa/nilable any?)) ;; Optional field
(sa/def ::maintenance_period int?)
(sa/def ::type string?)
(sa/def ::rental_price (sa/nilable any?))
(sa/def ::cover_image_id (sa/nilable any?))
(sa/def ::hand_over_note (sa/nilable any?))
(sa/def ::updated_at any?)
(sa/def ::internal_description (sa/nilable string?))
(sa/def ::product string?)
(sa/def ::info_url (sa/nilable any?))
(sa/def ::id uuid?) ;; UUID spec
(sa/def :any/id any?) ;; UUID spec
(sa/def ::manufacturer any?)
(sa/def ::version string?)
(sa/def ::created_at any?)
(sa/def ::technical_detail string?)

(sa/def :nil/technical_detail (sa/nilable string?))
(sa/def :nil/version (sa/nilable string?))
(sa/def :nil/description (sa/nilable string?))
(sa/def :nil/rental_price (sa/nilable string?))
(sa/def :nil/cover_image_id (sa/nilable string?))
(sa/def :nil/hand_over_note (sa/nilable string?))
(sa/def :nil/internal_description (sa/nilable string?))
(sa/def :nil/info_url (sa/nilable string?))

;; Define the full map spec
(def ResponseBodySoftware
  (sa/keys :req-un [:nil/description
                 ;::description
                    ::is_package
                    ::type
                    :nil/hand_over_note
                    :nil/internal_description
                    ::product
                    ::id
                    ::manufacturer
                    :nil/version
                    :nil/technical_detail]

           :opt-un [::attachments
                    ::maintenance_period
                    :nil/rental_price
                    :nil/cover_image_id
                    ::updated_at
                    :nil/info_url
                    ::created_at]))

;; Optional key

(sa/def ::post-license (sa/keys :req-un [::inventory_code]
                                :opt-un [::item_id
                                         :lr/id
                                         ::owner_id
                                         ::p4u
                                         ::total_quantity

                                         ::operating_system
                                         ::quantity_allocations
                                         ::maintenance_currency

                                         ::maintenance_price
                                         ::maintenance_expiration
                                         ::project_number

                                         ::license_expiration
                                         ::reference
                                         ::installation
                                         ::dongle_id
                                         ::procured_by
                                         ::maintenance_contract
                                         ::license_type
                                         ::activation_type

                                         ::is_borrowable
                                         :nil/retired

                                         ::is_inventory_relevant
                                         :nil/last_check

                                         :nil/shelf
                                         :nil/status_note
                                         :nil/name
                                         ::attachments
                                         :nil/invoice_number
                                         ::is_broken

                                         :nil/note
                                         :nil/serial_number

                                         ::updated_at
                                         :nil/retired_reason
                                         :nil/responsible
                                         :lr/invoice_date
                                         ::model_id
                                         :nil/supplier_id
                                         :nil/parent_id
                                         ::inventory_pool_id
                                         ::is_incomplete
                                         ::item_version
                                         ::needs_permission
                                         :nil/user_name
                                         ::room_id
                                         :lr/price
                                         ::created_at
                                         :nil/insurance_number]))

(def PackagePostPayload
  {:is_inventory_relevant boolean?
   :last_check any?
   :user_name (sa/nilable string?)
   :price (sa/nilable string?)
   :shelf (sa/nilable string?)
   :inventory_code string?
   :retired boolean?
   :is_broken boolean?
   :is_incomplete boolean?
   :is_borrowable boolean?
   :status_note (sa/nilable string?)
   :room_id uuid?
   :model_id uuid?
   :owner_id uuid?
   :items_attributes any?})


(defn get-model-by-pool-route []
  ["/:pool_id"

   {:swagger {:conflicting true
              :tags ["Models by pool"] :security []}}

   ["/item" ;; form/item new
    {:swagger {:conflicting true
               :tags ["form / item"] :security []}}

    [""
     {:post {:accept "application/json"
             :swagger {:consumes ["multipart/form-data"]
                       :produces "application/json"}
             :summary "(DEV) | Dynamic-Form-Handler: Fetch form data | Fetch fields by Role [v0]"
             :coercion spec/coercion
             :parameters {:path {:pool_id uuid? }
                          :multipart :item/multipart}
             :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
             :handler create-items-handler-by-pool-form
             :responses {200 {:description "OK"
                              :body test_ResponseBodySchema}
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}

      :get {:accept "application/json"
            :summary "(DEV) | Dynamic-Form-Handler: Fetch form data | Fetch fields by Role [v0]"
            :coercion spec/coercion
            :parameters {:path {:pool_id uuid?}}
            :handler fetch-items-handler-by-pool-form
            :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
            :responses {200 {:description "OK"
                             ;; TODO
                             :body ResponseBodySchema}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]]

   ["/models/:model_id/item" ;; new
    {:swagger {:conflicting true
               :tags ["form / item"] :security []}}

    ["/:item_id"
     {:put {:accept "application/json"
            :swagger {:consumes ["multipart/form-data"]
                      :produces "application/json"}
            :summary "(DEV) | Dynamic-Form-Handler: Fetch form data | Fetch fields by Role [v0]"
            :coercion spec/coercion
            :parameters {:path {:pool_id uuid?
                                :model_id uuid?
                                :item_id uuid?}
                         :multipart :item/multipart}
            :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
            :handler update-items-handler-by-pool-form
            :responses {200 {:description "OK"
                               :body {:data DataSchema2
                                    :validation [any?]}}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}

      :get {:accept "application/json" ;;new
            :summary "(DEV) | Dynamic-Form-Handler: Fetch form data [v0]"
            :coercion spec/coercion
            :parameters {:path {:pool_id uuid?
                                :model_id uuid?
                                :item_id uuid?}}
            :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
            :handler fetch-items-handler-by-pool-form
            :responses {200 {:description "OK"
                             :body {:data DataSchema2
                                    :fields [any?] }}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]]

   ["/package" ;; new
    {:swagger {:conflicting true
               :tags ["form / package"] :security []}}

    [""
     {:post {:accept "application/json"
             :swagger {:consumes ["multipart/form-data"]
                       :produces "application/json"}
             :summary "(DEV) | Dynamic-Form-Handler: Fetch form data | Fetch fields by Role [v0]"
             :coercion spec/coercion
             :parameters {:path {:pool_id uuid?  }
                          :multipart PackagePostPayload                          }
             :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
             :handler create-package-handler-by-pool-form
             :responses {200 {:description "OK"

                              :body :package-put-response2/inventory-item}

                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}

      :get {:accept "application/json"
            :summary "(DEV) | Dynamic-Form-Handler: Fetch form data | Fetch fields by Role [v0]"
            :coercion spec/coercion
            :parameters {:path {:pool_id uuid?}}
            :handler fetch-package-handler-by-pool-form
            :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
            :responses {200 {:body {:data {:inventory_code string?
                                           :inventory_pool_id uuid?
                                           :responsible_department any?}
                                    :fields [any?]}
                             :description "OK"}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]]

   ["/models/:model_id/package" ;; new
    {:swagger {:conflicting true
               :tags ["form / package"] :security []}}

    ["/:item_id"
     {:put {:accept "application/json"
            :swagger {:consumes ["multipart/form-data"]
                      :produces "application/json"}
            :summary "(DEV) | Dynamic-Form-Handler: Fetch form data | Fetch fields by Role [v0]"
            :coercion spec/coercion
            :parameters {:path {:pool_id uuid?
                                :model_id uuid?
                                :item_id uuid?}
                         :multipart PackagePostPayload} ;; TODO
            :handler update-package-handler-by-pool-form
            :responses { 200 {:description "OK"
                             :body :package-put-response2/inventory-item}
                        ;; FIXME
                             ;:body :package-put-response/inventory-item}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}

      :get {:accept "application/json" ;;new
            :summary "(DEV) | Dynamic-Form-Handler: Fetch form data [v0]"
            :coercion spec/coercion
            :parameters {:path {:pool_id uuid?
                                :model_id uuid?
                                :item_id uuid?}}
            :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
            :handler fetch-package-handler-by-pool-form
            :responses {200 {:description "OK"
                             :body :package-put-response2/inventory-item}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]]

   ["/model"
    {:swagger {:conflicting true
               :tags ["form / model"] :security []}}
    [""
     {:post {:accept "application/json"
             :swagger {:consumes ["multipart/form-data"]
                       :produces "application/json"}
             :summary "(DEV) | Form-Handler: Save data of 'Create model by form' | HERE??? [v0]"
             :description (str
                           " - Upload images and attachments \n"
                           " - Save data \n"
                           " - images: additional handling needed to process no/one/multiple files \n"
                           " - Browser creates thumbnails and attaches them as '*_thumb' \n\n\n"
                           " IMPORTANT\n - Upload of images with thumbnail (*_thumb) only")
             :coercion spec/coercion
             :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
             :parameters {:path {:pool_id uuid?}
                          :multipart ::multipart}
             :handler create-model-handler-by-pool-form
             :responses {200 {:description "OK"

                              :body {:data {:description (sa/nilable string?)
                                            :is_package boolean?
                                            :maintenance_period int?
                                            :type string?
                                            :rental_price (sa/nilable any?)
                                            :cover_image_id (sa/nilable any?)
                                            :hand_over_note (sa/nilable any?)
                                            :updated_at any?
                                            :internal_description (sa/nilable any?)
                                            :product string?
                                            :info_url (sa/nilable any?)
                                            :id uuid?
                                            :manufacturer any?
                                            :version string?
                                            :created_at any?
                                            :technical_detail string?}
                                     :validation any?}}

                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}}]

    ["/:model_id"
     [""
      {:get {:accept "application/json"
             :summary "(DEV) | Form-Handler: Fetch form data [v0]"
             :coercion spec/coercion
             :parameters {:path {:pool_id uuid?
                                 :model_id uuid?}}
             :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
             :handler create-model-handler-by-pool-form-fetch
             :responses {200 {:description "OK"
                              :body [{:description (sa/nilable string?)
                                      :properties any?
                                      :is_package boolean?
                                      :accessories any?
                                      :entitlement_groups any?

        ;; FIXME: causes error
                                      :images any?
        ;:images [{
        ;          :id (sa/nilable any?)
        ;          :filename (sa/nilable string?)
        ;          :content_type (sa/nilable string?)
        ;          :url (sa/nilable string?)
        ;          :thumbnail_url (sa/nilable string?)
        ;          :cover_image_id (sa/nilable string?)
        ;          }]

                                      :attachments [{:id (sa/nilable any?)
                                                     :filename (sa/nilable string?)
                                                     :content_type (sa/nilable string?)}]
                                      :type string?
                                      :hand_over_note (sa/nilable any?)
                                      :internal_description (sa/nilable any?)
                                      :product string?
                                      :categories [{:id (sa/nilable any?)
                                                    :type (sa/nilable string?)
                                                    :name (sa/nilable string?)}]
                                      :id uuid?
                                      :compatibles [{:id (sa/nilable any?)
                                                     :product (sa/nilable string?)}]
                                      :manufacturer any?
                                      :version string?
                                      :technical_detail string?}]}

                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}

       :put {:accept "application/json"
             :summary "(DEV) | [v0]"
             :swagger {:consumes ["multipart/form-data"]
                       :produces "application/json"}
             :coercion spec/coercion
             :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
             :parameters {:path {:pool_id uuid?
                                 :model_id uuid?}
                          :multipart ::multipart}
             :handler update-model-handler-by-pool-form
             :responses {200 {:description "OK"
                              :body [{:description (sa/nilable string?)
                                      :is_package boolean?
                                      :maintenance_period int?
                                      :type string?
                                      :rental_price (sa/nilable any?)
                                      :cover_image_id (sa/nilable any?)
                                      :hand_over_note (sa/nilable any?)
                                      :updated_at any?
                                      :internal_description (sa/nilable any?)
                                      :product string?
                                      :info_url (sa/nilable any?)
                                      :id uuid?
                                      :manufacturer any?
                                      :version string?
                                      :created_at any?
                                      :technical_detail string?}]}

                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}}]]]

   ["/option"
    {:swagger {:conflicting true
               :tags ["form / model"] :security []}}
    [""
     {:post {:accept "application/json"
             :swagger {:consumes ["multipart/form-data"]
                       :produces "application/json"}
             :summary "(DEV) | Form-Handler: Save data of 'Create model by form' | HERE??? [v0]"
             ;:description (str
             ;              " - Upload images and attachments \n"
             ;              " - Save data \n"
             ;              " - images: additional handling needed to process no/one/multiple files \n"
             ;              " - Browser creates thumbnails and attaches them as '*_thumb' \n\n\n"
             ;              " IMPORTANT\n - Upload of images with thumbnail (*_thumb) only")
             :coercion spec/coercion
             :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
             :parameters {:path {:pool_id uuid?}
                          :multipart :option/multipart}
             :handler create-option-handler-by-pool-form
             :responses {200 {:description "OK"
                              ;:body :res2/request ;; FIXME: shows key-prefixes
                              :body {:data {:product string?
                                            :inventory_pool_id uuid?
                                            :version string?
                                            :price any?
                                            :id uuid?
                                            :inventory_code string?}
                                     :validation any?}}

                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}}]

    ["/:option_id"
     [""
      {:get {:accept "application/json"
             :summary "(DEV) | Form-Handler: Fetch form data [v0]"
             :coercion spec/coercion
             :parameters {:path {:pool_id uuid?
                                 :option_id uuid?}}
             :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
             :handler fetch-option-handler-by-pool-form
             :responses {200 {:description "OK"
                              :body response-option  }
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}

       :put {:accept "application/json"
             :swagger {:consumes ["multipart/form-data"]
                       :produces "application/json"}
             :summary "(DEV) | [v0]"
             :coercion spec/coercion
             :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
             :parameters {:path {:pool_id uuid?
                                 :option_id uuid?}
                          :multipart :option/multipart}
             :handler update-option-handler-by-pool-form
             :responses {200 {:description "OK"
                              :content_type "multipart/form-data"
                              :body response-option                              }
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}}]]]

   ["/license" ;;new
    {:swagger {:conflicting true
               :tags ["form / licenses"] :security []}}

    [""
     {:post {:accept "application/json"
             :swagger {:consumes ["multipart/form-data"]
                       :produces "application/json"
                       :deprecated true}
             :summary "(DEV) | Dynamic-Form-Handler [v0]"
             :coercion spec/coercion
             :parameters {:path {:pool_id uuid?}
                          :multipart :license/multipart}
             :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
             :handler create-license-handler-by-pool-form
             :responses {200 {:description "OK"}
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}

      :get {:accept "application/json"
            :summary "(DEV) | Dynamic-Form-Handler: Fetch form data | Fetch fields by Role [v0]"
            :coercion spec/coercion
            :parameters {:path {:pool_id uuid?}}
            :handler fetch-license-handler-by-pool-form-fetch
            :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
            :responses {200 {:description "OK"
                             :body {:data ::FieldDataSchema
                                    :fields [::FieldSchema]}}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]

    ["/:model_id"
     [""
      {:get {:accept "application/json"
             :summary "(DEV) | Form-Handler: Fetch form data"
             :coercion spec/coercion
             :parameters {:path {:pool_id uuid?
                                 :model_id uuid?}}
             :handler fetch-license-handler-by-pool-form-fetch
             :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
             :swagger {:deprecated true}
             :responses {200 {:description "OK"}
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}

       :put {:accept "application/json"
             :swagger {:consumes ["multipart/form-data"]
                       :produces "application/json"
                       :deprecated true}
             :coercion spec/coercion
             :parameters {:path {:pool_id uuid?
                                 :model_id uuid?}
                          :multipart ::multipart}
             :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
             :handler update-license-handler-by-pool-form
             :responses {200 {:description "OK"}
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}}]]]

   ["/software"
    {:swagger {:conflicting true
               :tags ["form / software"] :security []}}
    [""
     {:post {:accept "application/json"
             :summary "(DEV) | Form-Handler: Fetch form data [v0]"
             :swagger {:consumes ["multipart/form-data"]
                       :produces "application/json"}
             :coercion spec/coercion
             :parameters {:path {:pool_id uuid?}
                          :multipart ::multipart}
             :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
             :handler create-software-handler-by-pool-form
             :responses {200 {:description "OK"
                              :body {:data ResponseBodySoftware
                                     :validation [any?]}}
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}}]

    ["/:model_id"
     [""
      {:get {:accept "application/json"
             :summary "(DEV) | Form-Handler: Fetch form data  [v0]"
             :coercion spec/coercion
             :parameters {:path {:pool_id uuid?
                                 :model_id uuid?}}
             :handler create-software-handler-by-pool-form-fetch
             :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
             :responses {200 {:description "OK"
                              :body [ResponseBodySoftware] }
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}

       :put {:accept "application/json"
             :swagger {:consumes ["multipart/form-data"]
                       :produces "application/json"}
             :summary "(DEV) | Form-Handler: Fetch form data [v0]"
             :coercion spec/coercion
             :parameters {:path {:pool_id uuid?
                                 :model_id uuid?}
                          :multipart ::multipart}
             :handler update-software-handler-by-pool-form
             :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
             :responses {200 {:description "OK"
                              :body [ResponseBodySoftware]}
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}}]]]

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
                                 (s/optional-key :filter_product) s/Str
                                 (s/optional-key :filter_ids) [s/Uuid]}}

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

     ["/licenses" ;; new
      {:swagger {:conflicting true
                 :tags ["form / licenses"] :security []}}

      [""
       {:post {:accept "application/json"
               :swagger {:consumes ["multipart/form-data"]
                         :produces "application/json"}
               :summary "(DEV) | Dynamic-Form-Handler: Fetch form data | Fetch fields by Role [v0]"
               :coercion spec/coercion
               :parameters {:path {:pool_id uuid?
                                   :model_id uuid?}
                            :multipart :license/multipart}
               :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
               :handler create-license-handler-by-pool-form
               :responses {200 {:description "OK"
                                :body {:data ::post-license
                                       :validation [any?]}}
                           404 {:description "Not Found"}
                           500 {:description "Internal Server Error"}}}}]

      ["/:item_id"
       {:put {:accept "application/json"
              :swagger {:consumes ["multipart/form-data"]
                        :produces "application/json"}
              :summary "(DEV) | Dynamic-Form-Handler: Fetch form data | Fetch fields by Role [v0]"
              :coercion spec/coercion
              :parameters {:path {:pool_id uuid?
                                  :model_id uuid?
                                  :item_id uuid?}
                           :multipart :license/multipart}
              :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
              :handler update-license-handler-by-pool-form
              :responses {200 {:description "OK"
                               :body [::post-license]}
                          404 {:description "Not Found"}
                          500 {:description "Internal Server Error"}}}

        :get {:accept "application/json" ;;new
              :summary "(DEV) | Dynamic-Form-Handler: Fetch form data [v0]"
              :coercion spec/coercion
              :parameters {:path {:pool_id uuid?
                                  :model_id uuid?
                                  :item_id uuid?}}
              :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
              :handler fetch-license-handler-by-pool-form-fetch
              :responses {200 {:description "OK"
                               :body {:data ::post-license
                                      :fields [any?]}}
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
