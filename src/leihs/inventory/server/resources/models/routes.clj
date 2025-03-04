(ns leihs.inventory.server.resources.models.routes
  (:require
   [clojure.spec.alpha :as sa]
   [leihs.core.core :refer [presence]]
   [leihs.inventory.server.resources.models.coercion :as mc]
   [leihs.inventory.server.resources.models.form.items.model-by-pool-form-create :refer [create-items-handler-by-pool-form]]
   [leihs.inventory.server.resources.models.form.items.model-by-pool-form-fetch :refer [fetch-items-handler-by-pool-form]]
   [leihs.inventory.server.resources.models.form.items.model-by-pool-form-update :refer [update-items-handler-by-pool-form]]
   [leihs.inventory.server.resources.models.form.license.model-by-pool-form-create :refer [create-license-handler-by-pool-form]]
   [leihs.inventory.server.resources.models.form.license.model-by-pool-form-fetch :refer [fetch-license-handler-by-pool-form-fetch]]
   [leihs.inventory.server.resources.models.form.license.model-by-pool-form-update :refer [update-license-handler-by-pool-form]]
   [leihs.inventory.server.resources.models.form.model.model-by-pool-form-create :refer [create-model-handler-by-pool-form]]
   [leihs.inventory.server.resources.models.form.model.model-by-pool-form-fetch :refer [create-model-handler-by-pool-form-fetch]]
   [leihs.inventory.server.resources.models.form.model.model-by-pool-form-update :refer [update-model-handler-by-pool-form
                                                                                         delete-model-handler-by-pool-form]]
   [leihs.inventory.server.resources.models.form.option.model-by-pool-form-create :refer [create-option-handler-by-pool-form]]
   [leihs.inventory.server.resources.models.form.option.model-by-pool-form-fetch :refer [fetch-option-handler-by-pool-form]]
   [leihs.inventory.server.resources.models.form.option.model-by-pool-form-update :refer [update-option-handler-by-pool-form]]
   [leihs.inventory.server.resources.models.form.package.model-by-pool-form-create :refer [create-package-handler-by-pool-form]]
   [leihs.inventory.server.resources.models.form.package.model-by-pool-form-fetch :refer [fetch-package-handler-by-pool-form]]
   [leihs.inventory.server.resources.models.form.package.model-by-pool-form-update :refer [update-package-handler-by-pool-form]]
   [leihs.inventory.server.resources.models.form.software.model-by-pool-form-create :refer [create-software-handler-by-pool-form]]
   [leihs.inventory.server.resources.models.form.software.model-by-pool-form-fetch :refer [create-software-handler-by-pool-form-fetch]]
   [leihs.inventory.server.resources.models.form.software.model-by-pool-form-update :refer [update-software-handler-by-pool-form
                                                                                            delete-software-handler-by-pool-form]]
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
   [leihs.inventory.server.resources.models.tree.filter :as filter]
   [leihs.inventory.server.resources.models.tree.tree :refer [tree]]
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

(defn get-model-route []
  ["/"
   {:swagger {:conflicting true
              :tags ["Models"]
              :security []}}

   ["manufacturers"
    {:get {:conflicting true
           :summary "Get manufacturers [v0]"
           :accept "application/json"
           :description "'search-term' works with at least one character, considers:\n
- manufacturer
- product
\nEXCLUDES manufacturers
- .. starting with space
- .. with empty string
\nHINT
- 'in-detail'-option works for models with set 'search-term' only\n"
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
                          :body mc/models-request-payload}
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
             :parameters {:path {:pool_id uuid?}
                          :multipart :item/multipart}
             :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
             :handler create-items-handler-by-pool-form
             :responses {200 {:description "OK"
                              :body mc/item-response-post}
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
                             :body mc/item-response-get}
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
                             :body {:data :item/response
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
                             :body {:data :item/response
                                    :fields [any?]}}
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
             :parameters {:path {:pool_id uuid?}
                          :multipart :package/payload}
             :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
             :handler create-package-handler-by-pool-form
             :responses {200 {:description "OK"
                              :body :package-put-response2/inventory-item}
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}

      :get {:accept "application/json"
            :summary "(DEV) | Dynamic-Form-Handler: Fetch form data | Fetch fields by Role [v0]"
            :description "Permitted access for:\n- lending_manager\n- inventory_manager"
            :coercion spec/coercion
            :parameters {:path {:pool_id uuid?}}
            :handler fetch-package-handler-by-pool-form
            :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
            :responses {200 {:body {:data {:inventory_code string?
                                           :inventory_pool_id uuid?
                                           :responsible_department any?}
                                    :fields [any?]}
                             :description "OK"}
                        401 {:description "Unauthorized: invalid role for the requested pool or method"
                             :body {:error string?}}
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
                         :multipart :package/payload}
            :handler update-package-handler-by-pool-form
            :responses {200 {:description "OK"
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
             :summary "(DEV) | Form-Handler: Save data of 'Create model by form' | [v0]"
             :description (str
                           " - Upload images and attachments \n"
                           " - Save data \n"
                           " - images: additional handling needed to process no/one/multiple files \n"
                           " - Browser creates thumbnails and attaches them as '*_thumb' \n\n\n"
                           " IMPORTANT\n - Upload of images with thumbnail (*_thumb) only")
             :coercion spec/coercion
             :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
             :parameters {:path {:pool_id uuid?}
                          :multipart :software/multipart}
             :handler create-model-handler-by-pool-form
             :responses {200 {:description "OK"
                              :body {:data :model-optional-response/inventory-model
                                     :validation any?}}
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}}]

    ["/"
     {:post {:accept "application/json"
             :summary "(DEV) | New JSON Endpoint (fake-version) | [v1]"
             :coercion spec/coercion
             :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
             :parameters {:path {:pool_id uuid?}
                          :body {:product string?}}
             :handler (fn [request]
                        (let [content-type (get-in request [:headers "content-type"])
                              _ (println ">o> content-type" content-type)
                              body (-> request :parameters :body)]
                          (cond
                            (= content-type "application/json") (response/response {:foo "bar" :body body})
                            :else {:status 400 :body "Unsupported Content-Type"})))
             :responses {200 {:description "OK"}
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
                              :body :model-strict-response/inventory-models}
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
                          :multipart :software/multipart}
             :handler update-model-handler-by-pool-form
             :responses {200 {:description "OK"
                              :body :model-optional-response/inventory-models}
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}

       :delete {:accept "application/json"
                :summary "(DEV) | Form-Handler: Delete form data [v0]"
                :swagger {:consumes ["multipart/form-data"]
                          :produces "application/json"}
                :coercion spec/coercion
                :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
                :parameters {:path {:pool_id uuid?
                                    :model_id uuid?}}
                :handler delete-model-handler-by-pool-form
                :responses {200 {:description "OK"
                                 :body {:deleted_attachments [{:id uuid?
                                                               :model_id uuid?
                                                               :filename string?
                                                               :size number?}]
                                        :deleted_images [any?]
                                        :deleted_model [{:id uuid?
                                                         :product string?
                                                         :manufacturer any?}]}}
                            404 {:description "Not Found"}
                            500 {:description "Internal Server Error"}}}}]]]

   ["/option"
    {:swagger {:conflicting true
               :tags ["form / option"] :security []}}
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
                                            :version (sa/nilable string?)
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
                              :body mc/response-option}
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
                              :body mc/response-option}
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
                             :body {:data :license/data-schema
                                    :fields [:license/field-schema]}}
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
                          :multipart :software/multipart}
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
                          :multipart :software/multipart}
             :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
             :handler create-software-handler-by-pool-form
             :responses {200 {:description "OK"
                              :body {:data :software/response
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
                              :body [:software/response]}
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}

       :put {:accept "application/json"
             :swagger {:consumes ["multipart/form-data"]
                       :produces "application/json"}
             :summary "(DEV) | Form-Handler: Fetch form data [v0]"
             :coercion spec/coercion
             :parameters {:path {:pool_id uuid?
                                 :model_id uuid?}
                          :multipart :software/multipart}
             :handler update-software-handler-by-pool-form
             :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
             :responses {200 {:description "OK"
                              :body [:software/response]}
                         404 {:description "Not Found"}
                         500 {:description "Internal Server Error"}}}

       :delete {:accept "application/json"
                :swagger {:consumes ["multipart/form-data"]
                          :produces "application/json"}
                :summary "(DEV) | Form-Handler: Delete form data [v0]"
                :coercion spec/coercion
                :parameters {:path {:pool_id uuid?
                                    :model_id uuid?}}
                :handler delete-software-handler-by-pool-form
                :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
                :responses {200 {:description "OK"
                                 :body {:deleted_attachments [{:id uuid?
                                                               :model_id uuid?
                                                               :filename string?
                                                               :size number?}]
                                        :deleted_model [{:id uuid?
                                                         :product string?
                                                         :manufacturer any?}]}}
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
                             :body (s/->Either [s/Any mc/models-response-payload])}
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
                                 :body (s/->Either [s/Any mc/models-response-payload])}
                            404 {:description "Not Found"}
                            500 {:description "Internal Server Error"}}}

          :put {:accept "application/json"
                :coercion reitit.coercion.schema/coercion
                :parameters {:path {:pool_id s/Uuid :model_id s/Uuid}
                             :body mc/models-request-payload}
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
                                     :model_id s/Uuid}}
                 :handler get-models-of-pool-with-pagination-handler
                 :responses {200 {:description "OK"
                                  ;:body (s/->Either [s/Any schema])} ;;FIXME
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
                               ;:body (s/->Either [s/Any schema])} ;;FIXME
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
                                :body {:data :license/post-license
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
                               :body [:license/post-license]}
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
                               :body {:data :license/post-license
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
                                     :model_id s/Uuid}}
                 :handler get-models-of-pool-with-pagination-handler
                 :responses {200 {:description "OK"
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
                               :body s/Any}

                          404 {:description "Not Found"}
                          500 {:description "Internal Server Error"}}}}]]]]])
