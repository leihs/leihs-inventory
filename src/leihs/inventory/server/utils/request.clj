(ns leihs.inventory.server.utils.request
  "Request-related utility functions.")

(def AUTHENTICATED_ENTITY :authenticated-entity)

(defn authenticated? [request]
  (-> request
      AUTHENTICATED_ENTITY
      boolean))

(defn get-auth-entity [request]
  (-> request
      AUTHENTICATED_ENTITY))

(defn path-params [request]
  (-> request :parameters :path))

(defn query-params [request]
  (-> request :parameters :query))

(defn body-params [request]
  (-> request :parameters :body))

(defn single-entity-get-request?
  "Check if request is a GET for a single entity (URI ends with UUID)."
  [request]
  (let [method (:request-method request)
        uri (:uri request)
        uuid-regex #"([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})$"]
    (and (= method :get)
         (boolean (re-find uuid-regex uri)))))
