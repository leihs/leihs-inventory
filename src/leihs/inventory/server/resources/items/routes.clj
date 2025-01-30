(ns leihs.inventory.server.resources.items.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.items.main :refer [get-items-of-pool-handler
                                                        get-items-of-pool-with-pagination-handler
                                                        get-items-handler]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [clojure.set :as set]

   [leihs.inventory.server.resources.utils.request :refer [query-params]]

   [leihs.inventory.server.resources.models.models-by-pool :refer [
                                                                   ;get-models-of-pool-handler
                                                                   ;create-model-handler-by-pool
                                                                   ;delete-model-handler-by-pool
                                                                   ;get-models-of-pool-auto-pagination-handler
                                                                   ;get-models-of-pool-handler
                                                                   ;get-models-of-pool-with-pagination-handler
                                                                   ;get-models-of-pool-auto-pagination-handler
                                                                   ;update-model-handler-by-pool

                                                                   get-models-handler
                                                                   ]]

   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [ring.util.response :as response]
   [cheshire.core :as json]

   [schema.core :as s]))


(defn merge-by-id [v1 v2 key]
  (vals
    (reduce (fn [acc item]
              (update acc (key item) merge item))
      {}
      (concat v1 v2))))

;(defn merge-by-id [v1 v2 key]
;  (->> (concat v1 v2)                   ;; Combine both vectors
;    (filter #(contains? % key))       ;; Ignore entries missing the key
;    (reduce (fn [acc item]
;              (update acc (key item) merge item))
;      {})
;    vals)) ;; Extract values from map

(defn merge-mn [v1 v2 key]
  (->> (concat v1 v2)                             ;; Combine vectors
    (filter #(contains? % key))                 ;; Ignore entries without the key
    (group-by key)                              ;; Group by key (M:N relationship)
    (map (fn [[k items]]
           (apply merge-with (fn [a b]
                               (if (coll? a)
                                 (conj a b)
                                 [a b])) items)))
    vec))  ;; Convert result back to a vector

(defn rename-key [data old-key new-key]
  (map #(set/rename-keys % {old-key new-key}) data))

(defn get-items-routes []
  [""

   [""
    {:swagger {:conflicting true
               :tags ["Items"] :security []}}

    ["/items"
     {:get {:conflicting true
            :accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :middleware [accept-json-middleware]
            :swagger {:produces ["application/json"]}
            :parameters {:query {(s/optional-key :page) s/Int
                                 (s/optional-key :size) s/Int}}
            :handler get-items-of-pool-with-pagination-handler
            :responses {200 {:description "OK"
                             :body s/Any}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]

    ["/items/:item_id"
     {:get {:conflicting true
            :accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :middleware [accept-json-middleware]
            :swagger {:produces ["application/json"]}
            :parameters {:path {:item_id s/Uuid}}
            :handler get-items-of-pool-handler
            :responses {200 {:description "OK"
                             :body s/Any}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]]

   ["/:pool_id"
    {:swagger {:conflicting true
               :tags ["Items by pool"] :security []}}



    ["/items-with-model-info"
     {:get {:description "Shortcut to fetch Items with model info (distinct item entries) page&size set!!"
            :conflicting true
            :accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :middleware [accept-json-middleware]
            :swagger {:produces ["application/json"]}
            :parameters {:path {:pool_id s/Uuid}
                         :query {
                                 (s/optional-key :page) s/Int
                                 (s/optional-key :size) s/Int

                                 (s/optional-key :search_term) s/Str
                                 :result_type (s/enum "Normal" "Min")

                                 }}
            :handler (fn [request]


                          (let [

                                ;; request items
                                result-type (get-in request [:parameters :query :result_type])

                                request (update-in request [:parameters :query] merge {
                                                                                       ;:size 200 :page 1
                                                                                       :not_packaged true :packages false :retired false :result_type "Distinct"})
                                res1 (get-items-handler request true)
                                ;; TODO: handle empty result _> no result "abc"

                                ;; prepare and request models
                                ids (vec (flatten (map :model_id res1)))
                                request (-> request
                                             (assoc-in [:parameters :query] {})
                                             (assoc-in [:parameters :path] {})
                                             (update-in [:parameters :query] merge {:paginate false :filter_ids ids}))

                                res2 (get-models-handler request false)

                                res2 (map #(select-keys % [:id :product :manufacturer]) res2)
                                res2 (rename-key res2 :id :model_id)

                                res3 (merge-by-id res1 res2 :model_id)
                                res4 (map #(select-keys % [:inventory_code :product]) res3)



                                result (if (= "Normal" result-type)
                                         res3
                                         res4
                                         )

                                ;result res4
                                   ]

                            (-> (response/response result)
                              (response/header "Count" (count result)))
                            )

)


            :responses {200 {:description "OK"
                             :body [{:inventory_code s/Str
                                     :product s/Str

                                     (s/optional-key :is_package)        s/Bool
                                      (s/optional-key :retired)           s/Any
                                      (s/optional-key :model_id)          s/Uuid
                                      (s/optional-key :parent_id)         (s/maybe s/Uuid)
                                      (s/optional-key :id)                s/Uuid
                                      (s/optional-key :inventory_pool_id) s/Uuid
                                      (s/optional-key :manufacturer)      (s/maybe s/Str)
                                     }]
                             ;:body s/Any
                              }

                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]

    ["/items"
     {:get {:description "https://staging.leihs.zhdk.ch/manage/8bd16d45-056d-5590-bc7f-12849f034351/items \n\n

     "
            :conflicting true
            :accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :middleware [accept-json-middleware]
            :swagger {:produces ["application/json"]}
            :parameters {:path {:pool_id s/Uuid}
                         :query {(s/optional-key :page) s/Int
                                 (s/optional-key :size) s/Int

                                 (s/optional-key :search_term) s/Str
                                 (s/optional-key :not_packaged) s/Bool
                                 (s/optional-key :packages) s/Bool
                                 (s/optional-key :retired) s/Bool

                                 :result_type (s/enum "Min" "Normal" "Distinct")
                                 }}
            :handler get-items-of-pool-with-pagination-handler
            :responses {200 {:description "OK"
                             :body s/Any}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]

    ["/items/:item_id"
     {:get {:conflicting true
            :accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :middleware [accept-json-middleware]
            :swagger {:produces ["application/json"]}
            :parameters {:path {:pool_id s/Uuid :item_id s/Uuid}}
            :handler get-items-of-pool-handler
            :responses {200 {:description "OK"
                             :body s/Any}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]]])
