(ns leihs.inventory.server.resources.pool.items.routes
  (:require
   [leihs.inventory.server.resources.pool.items.main :as items]
   [leihs.inventory.server.resources.pool.items.types :refer [query-params query-params-advanced]]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  ["/items/"
   ["" {:get {:accept "application/json"
              :coercion reitit.coercion.schema/coercion
              :swagger {:produces ["application/json"]}
              :parameters {:path {:pool_id s/Uuid}
                           :query query-params}
              :handler items/index-resources
              :produces ["application/json"]
              :responses {200 {:description "OK"
                               :body s/Any}
                          ;:body get-items-response} ;; FIXME broken
                          404 {:description "Not Found"}
                          500 {:description "Internal Server Error"}}}





        :post {:description "Create a new item. Fields starting with 'properties_' are stored in the properties JSONB column, others in their respective item columns."
               :accept "application/json"
               :coercion reitit.coercion.schema/coercion
               :swagger {:produces ["application/json"]}
               :parameters {:path {:pool_id s/Uuid}
                            :body {(s/optional-key :inventory_code) s/Str
                                   (s/optional-key :model_id) s/Uuid
                                   (s/optional-key :room_id) s/Uuid
                                   (s/optional-key :inventory_pool_id) s/Uuid
                                   (s/optional-key :owner_id) s/Uuid
                                   s/Keyword s/Any}}
               :handler items/post-resource
               :responses {200 {:description "OK"
                                :body s/Any}
                           400 {:description "Bad Request"}
                           404 {:description "Not Found"}
                           500 {:description "Internal Server Error"}}}

        :patch {:description "Update items used by AdvancedSearch"
                :accept "application/json"
                :coercion reitit.coercion.schema/coercion
                :swagger {:produces ["application/json"]}
                :parameters {:path {:pool_id s/Uuid}
                             :body {
                                    :ids [s/Uuid]
                                    :data {
                                           ;(s/optional-key :ampere) s/Str
                                           (s/optional-key :properties_ampere) s/Str
                                           (s/optional-key :is_borrowable) s/Bool
                                           (s/optional-key :room_id) s/Uuid
                                           (s/optional-key :is_incomplete) s/Bool
                                           (s/optional-key :properties_imei_number) s/Str

                                           (s/optional-key :price) s/Str
                                           (s/optional-key :inventory_code) s/Str

                                           ;(s/optional-key :invoice_date) s/Str
                                           (s/optional-key :invoice_date) java.time.LocalDate
                                           (s/optional-key :invoice_number) s/Str
                                           ;(s/optional-key :last_check) s/Str
                                           (s/optional-key :last_check) java.time.LocalDate
                                           (s/optional-key :model_id) s/Str
                                           (s/optional-key :note) s/Str
                                           (s/optional-key :owner_id) s/Uuid

                                           (s/optional-key :properties_p4u) s/Str
                                           (s/optional-key :properties_reference) s/Str
                                           (s/optional-key :is_inventory_relevant) s/Bool
                                           ;(s/optional-key :responsible) s/Str
                                           (s/optional-key :responsible) (s/maybe s/Str)

                                           (s/optional-key :retired) s/Str
                                           (s/optional-key :retired_reason) s/Str

                                           (s/optional-key :serial_number) s/Str
                                           (s/optional-key :shelf) s/Str
                                           (s/optional-key :status_note) s/Str

                                           (s/optional-key :supplier_id) s/Uuid
                                           (s/optional-key :user_name) s/Str

                                           (s/optional-key :properties_electrical_power) s/Str
                                           (s/optional-key :properties_warranty_expiration) s/Str
                                           (s/optional-key :is_broken) s/Bool
                                           }
                                    }}
                :handler items/patch-resource
                :responses {200 {:description "OK"
                                 :body s/Any}
                            400 {:description "Bad Request"}
                            404 {:description "Not Found"}
                            500 {:description "Internal Server Error"}}}}]


   ["advanced/"
    {:get {:accept "application/json"
           :coercion reitit.coercion.schema/coercion

           :describetion "TODO"

           :swagger {:produces ["application/json"]}
           :parameters {:path {:pool_id s/Uuid}
                        :query query-params-advanced}
           :handler items/advanced-index-resources
           :produces ["application/json"]
           :responses {200 {:description "OK"
                            :body s/Any}
                       ;:body get-items-response} ;; FIXME broken
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]

   ]

  )
