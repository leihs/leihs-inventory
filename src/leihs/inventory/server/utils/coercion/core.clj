(ns leihs.inventory.server.utils.coercion.core
  (:require
   [clojure.string :as str]
   [ring.util.response :refer [response status]]

   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]
   ))

(def pagination {                    :total_records s/Int
                    :current_page s/Int
                    :per_page s/Int
                    :total_pages s/Int
                    :next_page (s/maybe s/Int)
                    :prev_page (s/maybe s/Int)})