(ns leihs.inventory.server.resources.pool.items.types
  (:require
   ;[cheshire.core :as json]
   ;[clojure.set :as set]
   ;[leihs.inventory.server.resources.pool.items.main :refer [get-items-of-pool-with-pagination-handler]]
   ;[leihs.inventory.server.resources.pool.models.main :refer [get-models-handler]]
   ;[leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   ;[leihs.inventory.server.resources.utils.request :refer [query-params]]
   ;[leihs.inventory.server.utils.response_helper :as rh]
   ;[reitit.coercion.schema]
   ;[reitit.coercion.spec]
   ;[ring.middleware.accept]
   ;[ring.util.response :as response]
   [schema.core :as s]
   ))


(s/defschema query-params {(s/optional-key :page) s/Int
                                 (s/optional-key :size) s/Int
                                 (s/optional-key :search_term) s/Str
                                 (s/optional-key :not_packaged) s/Bool
                                 (s/optional-key :packages) s/Bool
                                 (s/optional-key :retired) s/Bool
                                 :result_type (s/enum "Min" "Normal" "Distinct")})