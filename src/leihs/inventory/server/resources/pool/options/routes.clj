(ns leihs.inventory.server.resources.pool.options.routes
  (:require
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.options.main :as options]
   [leihs.inventory.server.resources.pool.options.types :refer [response-option-get
                                                                response-option-post
                                                               ]]


   [leihs.inventory.server.utils.auth.role-auth :refer [permission-by-role-and-pool]]
   [leihs.inventory.server.utils.auth.roles :as roles]
   [leihs.inventory.server.utils.coercion.core :refer [Date]]
   [leihs.inventory.server.utils.middleware :refer [accept-json-middleware]]
   [reitit.coercion.schema]
   [clojure.spec.alpha :as sa]
   [reitit.coercion.spec :as spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  ["/options/"
   {
    :post {:accept "application/json"
           :swagger {:consumes ["multipart/form-data"]
                     :produces "application/json"}
           :summary "(DEV) | Form-Handler: Save data of 'Create model by form' | [v0]"
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
           :handler options/create-option-handler-by-pool-form
           :responses {200 {:description "OK"
                            :body response-option-post          }
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}

    :get {:accept "application/json"
          :summary "(DEV) | Form-Handler: Fetch form data [v0]"
          :coercion spec/coercion
          :parameters {:path {:pool_id uuid?}}
          :middleware [(permission-by-role-and-pool roles/min-role-lending-manager)]
          :handler options/fetch-option-handler-by-pool-form
          :responses {200 {:description "OK"
                           :body response-option-get
                           }
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}

    }])
