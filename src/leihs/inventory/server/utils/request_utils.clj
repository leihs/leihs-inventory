(ns leihs.inventory.server.utils.request-utils)

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
