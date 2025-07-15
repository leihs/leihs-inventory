(ns leihs.inventory.server.resources.pool.suppliers.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.suppliers.main :as suppliers]
   [leihs.inventory.server.resources.pool.suppliers.types :refer [get-response]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.coercion.core :refer [pagination]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  [""
   ["/:pool_id/suppliers/"
    {:swagger {:tags [""]}}
    ["" {:get {:summary (fe "a.k.a 'Lieferanten'")
               :description (str
                             "- DEFAULT: no pagination\n"
                             "- OK-Legacy | "
                             "Form: https://staging.leihs.zhdk.ch/manage/8bd16d45-056d-5590-bc7f-12849f034351/fields?target_type=itemRequest")
               :accept "application/json"
               :coercion reitit.coercion.schema/coercion
               :middleware [accept-json-middleware
                            ;session/wrap
                            ]
               :swagger {:produces ["application/json"]}
               :parameters {:query {(s/optional-key :page) s/Int
                                    (s/optional-key :size) s/Int
                                    (s/optional-key :search-term) s/Str}}
               :handler suppliers/index-resources
               :responses {200 {:description "OK"
                                :body get-response}
                           404 {:description "Not Found"}
                           500 {:description "Internal Server Error"}}}}]]])
