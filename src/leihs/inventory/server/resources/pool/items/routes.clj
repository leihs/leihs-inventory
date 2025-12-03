(ns leihs.inventory.server.resources.pool.items.routes
  (:require
   [leihs.inventory.server.resources.pool.items.filter-handler :as fh]
   [leihs.inventory.server.resources.pool.items.main :as items]
   [leihs.inventory.server.resources.pool.items.types :as types :refer [query-params]]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []

  ["/" ["items/"
        {:get {:accept "application/json"
               :coercion reitit.coercion.schema/coercion
               :swagger {:produces ["application/json"]}
               :summary "Returns all items/packages of a pool filtered by query parameters"
               :parameters {:path types/path-params
                            :query types/query-params}
               :handler items/index-resources
               :produces ["application/json"]
               :responses {200 {:description "OK"
                           ;:body types/get-items-response ;; FIXME
                                }
                           400 {:description "Bad Request"
                                :body types/error-body}
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

         :patch {:description "Batch-Update of items (used by AdvancedSearch)"
                 :accept "application/json"
                 :coercion reitit.coercion.schema/coercion
                 :swagger {:produces ["application/json"]}
                 :parameters {:path {:pool_id s/Uuid}
                              :body {:ids [s/Uuid]
                                     :data {(s/optional-key :properties_ampere) s/Str
                                            (s/optional-key :is_borrowable) s/Bool
                                            (s/optional-key :room_id) s/Uuid
                                            (s/optional-key :is_incomplete) s/Bool
                                            (s/optional-key :properties_imei_number) s/Str

                                            (s/optional-key :price) s/Str
                                            (s/optional-key :inventory_code) s/Str

                                            (s/optional-key :invoice_date) java.time.LocalDate
                                            (s/optional-key :invoice_number) s/Str
                                            (s/optional-key :last_check) java.time.LocalDate
                                            (s/optional-key :model_id) s/Str
                                            (s/optional-key :note) s/Str
                                            (s/optional-key :owner_id) s/Uuid

                                            (s/optional-key :properties_p4u) s/Str
                                            (s/optional-key :properties_reference) s/Str
                                            (s/optional-key :is_inventory_relevant) s/Bool
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
                                            (s/optional-key :is_broken) s/Bool}}}

                 :handler items/patch-resource
                 :responses {200 {:description "OK"
                                  :body s/Any}
                             400 {:description "Bad Request"
                                  :body types/error-body}
                             404 {:description "Not Found"}
                             500 {:description "Internal Server Error"}}}}]

   ["items-filter/"
    {:get {:accept "application/json"
           :summary "DEV - TODO: remove endpoint"
           :description "\n- resource_id .. id of item/licence

\nLogic: | .. AND,      || .. OR
\nLogic: A | B || C  →  (A AND B) OR C.
\n----------------------------------------------------
\n<b>Filter examples:</b>
\ninventory_code~~ITZ211
\nuser_name~~itz
\nmodel_id~21c2b7ac-0645-5d7e-8b17-ad8047ca89da
\nretired~true
\nretired_reason isnull
\n
\n<b>Date filters:</b>
\ninvoice_date~2013-09-19                     (exact date)
\ninvoice_date>2013-09-19                     (after)
\ninvoice_date<2024-12-31                     (before)
\ninvoice_date>=2013-09-19                    (on/after)
\ninvoice_date<=2024-12-31                    (on/before)
\ninvoice_date~2013-09-19;2013-09-20          (range)
\nlast_check>2024-01-01
\n
\n<b>Combinations:</b>
\nis_incomplete~false || is_incomplete~true | is_broken~true
\ninvoice_date~2013-09-19;2013-09-20 | is_broken~false
\ninvoice_date>=2013-09-19 | invoice_date<=2013-09-20
\n
\n<b>Other filters:</b>
\nproperties_ampere~12A
\nproperties_ampere~~12
\nproperties_imei_number~~C-Band
\nowner_id~8bd16d45-056d-5590-bc7f-12849f034351
\nroom_id~286e48aa-c1cd-5550-a3ff-736ae8f76179
\nsupplier_id~64385d0b-83de-51b1-8a87-ef8621030bf7
\n
\n<b>Operators:</b> ~ (equals) ~~ (contains, ilike) > < >= <= ! isnull isnotnull
"
           :coercion reitit.coercion.schema/coercion
           :parameters {:path {:pool_id s/Uuid}
                        :query {;:resource_id s/Uuid
                               ;(s/optional-key :filter) [s/Str]}}
                                (s/optional-key :filter) s/Str}}

           :handler (fn [request]
                      (fh/list-pool-items request))

           :responses {200 {:description "OK"}
                       400 {:description "Bad Request"
                            :body types/error-body}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]])
