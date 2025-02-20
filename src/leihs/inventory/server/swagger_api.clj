(ns leihs.inventory.server.swagger-api
  (:require [byte-streams :as bs]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.walk :refer [keywordize-keys]]
            [leihs.core.anti-csrf.back :as anti-csrf]
            [leihs.core.auth.core :as auth]
            [leihs.core.auth.session :as session]
            [leihs.core.db :as db]
            [leihs.core.ring-audits :as ring-audits]
            [leihs.core.routing.back :as core-routing]
            [leihs.core.routing.dispatch-content-type :as dispatch-content-type]
            [leihs.inventory.server.constants :as consts]
            [leihs.inventory.server.resources.auth.session :refer [get-cookie-value]]
            [leihs.inventory.server.routes :as routes]
            [leihs.inventory.server.utils.csrf-handler :as csrf]
            [leihs.inventory.server.utils.response_helper :as rh]
            [leihs.inventory.server.utils.ressource-handler :refer [custom-not-found-handler]]
            [leihs.inventory.server.utils.session-dev-mode :as dm]
            [muuntaja.core :as m]
            [reitit.coercion.schema]
            [reitit.coercion.spec]
            [reitit.dev.pretty :as pretty]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.exception :as exception]
            [reitit.ring.middleware.multipart :as multipart]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.util.codec :as codec]
            [ring.util.response :as response]))

(defn valid-image-or-thumbnail-uri? [uri]
  (boolean (re-matches #"^/inventory/images/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}(/thumbnail)?$" uri)))

(defn valid-attachment-uri? [uri]
  (boolean (re-matches #"^/inventory/attachments/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$" uri)))

(defn default-handler-fetch-resource [handler]
  (fn [request]
    (let [accept-header (get-in request [:headers "accept"])
          uri (:uri request)
          whitelist-uris-for-api ["/sign-in" "/sign-out"]
          image-or-thumbnail-request? (valid-image-or-thumbnail-uri? uri)
          attachment-request? (valid-attachment-uri? uri)]

      (if (or (and accept-header (some #(str/includes? accept-header %) ["openxmlformats" "text/csv" "json" "image/jpeg"]))
              (some #(= % uri) whitelist-uris-for-api)
              image-or-thumbnail-request?
              attachment-request?)
        (handler request)
        (custom-not-found-handler request)))))

(defn wrap-accept-with-image-rewrite [handler]
  (fn [request]
    (let [accept-header (get-in request [:headers "accept"])
          uri (:uri request)
          updated-request (cond
                            (and (or (str/includes? accept-header "text/html") (str/includes? accept-header "image/*"))
                                 (valid-image-or-thumbnail-uri? uri))
                            (assoc-in request [:headers "accept"] "image/jpeg")

                            (and (str/includes? accept-header "text/html")
                                 (valid-attachment-uri? uri))
                            (assoc-in request [:headers "accept"] "application/octet-stream")

                            :else request)]
      ((dispatch-content-type/wrap-accept handler) updated-request))))

(defn create-app [options]
  (let [router (ring/router

                (routes/basic-routes)

                {:conflicts nil
                 :exception pretty/exception
                 :data {:coercion reitit.coercion.spec/coercion
                        :muuntaja m/instance
                        :middleware [db/wrap-tx
                                     core-routing/wrap-canonicalize-params-maps
                                     muuntaja/format-middleware
                                     ring-audits/wrap
                                     wrap-accept-with-image-rewrite

                                     csrf/extract-header
                                     session/wrap-authenticate
                                     wrap-cookies
                                     csrf/wrap-csrf
                                     dm/extract-dev-cookie-params

                                      ;locale/wrap
                                      ;settings/wrap
                                      ;datasource/wrap-tx
                                      ;wrap-json-response
                                      ;(wrap-json-body {:keywords? true})
                                      ;wrap-empty
                                      ;wrap-form-params

                                     wrap-params
                                     wrap-content-type

                                     dispatch-content-type/wrap-accept

                                     default-handler-fetch-resource
                                     csrf/wrap-dispatch-content-type

                                     swagger/swagger-feature
                                     parameters/parameters-middleware
                                     muuntaja/format-negotiate-middleware
                                     muuntaja/format-response-middleware
                                     exception/exception-middleware
                                     muuntaja/format-request-middleware
                                     coercion/coerce-response-middleware
                                     coercion/coerce-request-middleware
                                     multipart/multipart-middleware]}})]

    (-> (ring/ring-handler
         router
         (ring/routes
          (ring/redirect-trailing-slash-handler {:method :strip})

          (swagger-ui/create-swagger-ui-handler
           {:path "/inventory/api-docs/"
            :config {:validatorUrl nil
                     :urls [{:name "swagger" :url "swagger.json"}]
                     :urls.primaryName "openapi"
                     :operationsSorter "alpha"}})

          (ring/create-default-handler
           {:not-found (default-handler-fetch-resource custom-not-found-handler)}))))))
