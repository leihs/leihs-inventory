(ns leihs.inventory.server.utils.coercion.core
  (:require
   [clojure.spec.alpha :as sa]
   [clojure.string :as str]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [ring.util.response :refer [response status]]
   [schema.core :as s]))

(def pagination {:total_records s/Int
                 :current_page s/Int
                 :per_page s/Int
                 :total_pages s/Int
                 :next_page (s/maybe s/Int)
                 :prev_page (s/maybe s/Int)})