(ns leihs.inventory.server.resources.pool.manufacturers.routes
  (:require
   [clojure.spec.alpha :as sa]
   [clojure.string :as str]
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.manufacturers.main :as manufacturers]
   [leihs.inventory.server.resources.pool.manufacturers.types :refer [response-schema]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.auth.role-auth :refer [permission-by-role-and-pool]]
   [leihs.inventory.server.utils.auth.roles :as roles]
   [leihs.inventory.server.utils.coercion.core :refer [Date]]
   [leihs.inventory.server.resources.pool.models.model.constants :refer [config-get]]
   [reitit.coercion.schema]
   [reitit.coercion.spec :as spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  ["/:pool_id/"
   {:swagger {:tags [""]}}

   ["manufacturers/"
    {:get {:summary (fe "")
           :accept "application/json"
           :description "'search-term' works with at least one character, considers:\n
- manufacturer
- product
\nEXCLUDES manufacturers
- .. starting with space
- .. with empty string
\nHINT
- 'in-detail'-option works for models with set 'search-term' only\n"
           :coercion reitit.coercion.schema/coercion
           :middleware [accept-json-middleware]
           :swagger {:produces ["application/json"]}
           :handler manufacturers/index-resources
           :parameters {:query {(s/optional-key :type) (s/enum "Software" "Model")
                                (s/optional-key :search-term) s/Str
                                (s/optional-key :in-detail) (s/enum "true" "false")}}
           :responses {200 {:description "OK"
                            :body response-schema}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]])
