(ns leihs.inventory.server.swagger-api
  (:require [reitit.ring :as ring]
     [reitit.coercion.spec]
     [reitit.openapi :as openapi]
     [reitit.swagger :as swagger]
     [reitit.swagger-ui :as swagger-ui]
     [reitit.ring.coercion :as coercion]
     [reitit.dev.pretty :as pretty]
     [reitit.ring.middleware.muuntaja :as muuntaja]
     [reitit.ring.middleware.exception :as exception]
     [reitit.ring.middleware.multipart :as multipart]
     [reitit.ring.middleware.parameters :as parameters]
     [spec-tools.core :as st]
     [ring.adapter.jetty :as jetty]
     [muuntaja.core :as m]
     [clojure.spec.alpha :as s]
     [clojure.java.io :as io]
     [cheshire.core :as json])
    (:import (java.util UUID)))

  ;; Define specs for person data with descriptions and default values
  (s/def ::uuid (st/spec {:spec uuid?
                          :description "A unique identifier for the person"
                          :json-schema/default (str (UUID/randomUUID))}))

  (s/def ::firstname (st/spec {:spec string?
                               :description "The first name of the person"}))

  (s/def ::lastname (st/spec {:spec string?
                              :description "The last name of the person"}))

  (s/def ::age (st/spec {:spec pos-int?
                         :description "The age of the person"
                         :json-schema/default 0}))

  (s/def ::person (s/keys :req-un [::uuid ::firstname ::lastname ::age]))
  (s/def ::person-list (s/coll-of ::person))

  ;; Define specs for pagination
  (s/def ::page (st/spec {:spec pos-int?
                          :description "Page number"
                          :json-schema/default 1}))

  (s/def ::size (st/spec {:spec pos-int?
                          :description "Number of items per page"
                          :json-schema/default 10}))

  ;; In-memory database for person data
  (def person-db (atom []))

  ;; Helper functions
  (defn add-person [person]
    (swap! person-db conj person))

  (defn get-person [id]
    (some #(when (= (:uuid %) id) %) @person-db))

  (defn update-person [id person]
    (swap! person-db #(mapv (fn [p] (if (= (:uuid p) id) person p)) %)))

  (defn delete-person [id]
    (swap! person-db (fn [persons] (remove #(= (:uuid %) id) persons))))

  (defn paginate [data page size]
    (let [start (* (dec page) size)
          end (min (+ start size) (count data))]
      (subvec data start end)))

  (def app
    (ring/ring-handler
      (ring/router
        [["/swagger.json"
          {:get {:no-doc true
                 :swagger {:info {:title "my-api"
                                  :version "2.0.0"}}
                 :handler (swagger/create-swagger-handler)}}]
         ["/openapi.json"
          {:get {:no-doc true
                 :openapi {:openapi "3.0.0"
                           :info {:title "my-api"
                                  :description "openapi3-docs with reitit-http"
                                  :version "3.0.0"}}
                 :handler (openapi/create-openapi-handler)}}]

         ["/person"
          {:tags ["person"]}

          ["/all"
           {:get {:summary "Get all persons with pagination"
                  :parameters {:query {:page ::page :size ::size}}
                  :responses {200 {:body ::person-list}}
                  :handler (fn [{{{:keys [page size]} :query} :parameters}]
                             (let [paginated-data (paginate @person-db page size)]
                               {:status 200
                                :body paginated-data}))}}]

          ["/"
           {:post {:summary "Add a new person"
                   :parameters {:body ::person}
                   :responses {201 {:body ::person}}
                   :handler (fn [{{:keys [body]} :parameters}]
                              (add-person body)
                              {:status 201
                               :body body})}}]

          ["/{id}"
           {:get {:summary "Get a person by ID"
                  :parameters {:path {:id ::uuid}}
                  :responses {200 {:body ::person}}
                  :handler (fn [{{:keys [path]} :parameters}]
                             (if-let [person (get-person (:id path))]
                               {:status 200
                                :body person}
                               {:status 404
                                :body {:error "Person not found"}}))}
            :put {:summary "Update a person by ID"
                  :parameters {:path {:id ::uuid}
                               :body ::person}
                  :responses {200 {:body ::person}}
                  :handler (fn [{{:keys [path body]} :parameters}]
                             (update-person (:id path) body)
                             {:status 200
                              :body body})}
            :delete {:summary "Delete a person by ID"
                     :parameters {:path {:id ::uuid}}
                     :responses {204 {:description "No Content"}}
                     :handler (fn [{{:keys [path]} :parameters}]
                                (delete-person (:id path))
                                {:status 204})}}]]]

        {;;:reitit.middleware/transform dev/print-request-diffs ;; pretty diffs
         ;;:validate spec/validate ;; enable spec validation for route data
         ;;:reitit.spec/wrap spell/closed ;; strict top-level validation
         :exception pretty/exception
         :data {:coercion reitit.coercion.spec/coercion
                :muuntaja m/instance
                :middleware [;; swagger feature
                             swagger/swagger-feature
                             ;; query-params & form-params
                             parameters/parameters-middleware
                             ;; content-negotiation
                             muuntaja/format-negotiate-middleware
                             ;; encoding response body
                             muuntaja/format-response-middleware
                             ;; exception handling
                             exception/exception-middleware
                             ;; decoding request body
                             muuntaja/format-request-middleware
                             ;; coercing response bodys
                             coercion/coerce-response-middleware
                             ;; coercing request parameters
                             coercion/coerce-request-middleware
                             ;; multipart
                             multipart/multipart-middleware]}
         :conflicts nil}) ;; Ignore route conflicts
      (ring/routes

        (swagger-ui/create-swagger-ui-handler
          {:path "/"
           :config {:validatorUrl nil
                    :urls [{:name "swagger" :url "swagger.json"}
                           {:name "openapi" :url "openapi.json"}]
                    :urls.primaryName "openapi"
                    :operationsSorter "alpha"}})
        (ring/create-default-handler)

        )))

  (defn main [& args]
    (jetty/run-jetty #'app {:port 4000, :join? false})
    ;

    (println "server running in port 4000"))

  (comment
    (start))
