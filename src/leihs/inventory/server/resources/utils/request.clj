(ns leihs.inventory.server.resources.utils.request
  (:require [clojure.string :as str]))

(def AUTHENTICATED-ENTITY :authenticated-entity )

(defn authenticated? [request]
  (-> request
      AUTHENTICATED-ENTITY
      boolean))

(defn get-auth-entity [request]
  (-> request
      AUTHENTICATED-ENTITY))

(defn not-authenticated? [request]
  (-> request
      AUTHENTICATED-ENTITY
      boolean not))
