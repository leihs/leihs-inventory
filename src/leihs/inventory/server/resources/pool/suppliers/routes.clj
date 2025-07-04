(ns leihs.inventory.server.resources.pool.suppliers.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.constants :refer [fe]]
   ;[leihs.inventory.server.resources.auth.session :as session]
   [leihs.inventory.server.resources.pool.suppliers.main :refer [get-suppliers-auto-pagination-handler]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.coercion.core :refer [pagination]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(def resp-supplier [{:id s/Uuid
                     :name s/Str
                     :note (s/maybe s/Str)}])

(defn get-suppliers-routes []
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

               :handler get-suppliers-auto-pagination-handler
               :responses {200 {:description "OK"
                                :body (s/->Either [resp-supplier {:data resp-supplier
                                                                  :pagination pagination}])}
                           404 {:description "Not Found"}
                           500 {:description "Internal Server Error"}}}}]]])
