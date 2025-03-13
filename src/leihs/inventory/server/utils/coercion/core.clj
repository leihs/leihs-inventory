(ns leihs.inventory.server.utils.coercion.core
  (:require
   [clojure.spec.alpha :as sa]
   [clojure.string :as str]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [ring.util.response :refer [response status]]
   [schema.core :as s]))

(def pagination {:size s/Int
                 :page s/Int
                 :total_rows s/Int
                 :total_pages s/Int})