(ns leihs.inventory.server.resources.items.routes
  (:require
   [cheshire.core :as json]
   [clojure.set]
   [clojure.set :as set]
   [leihs.inventory.server.resources.items.main :refer [get-items-of-pool-handler
                                                        get-items-of-pool-with-pagination-handler
                                                        get-items-handler]]
   [leihs.inventory.server.resources.models.models-by-pool :refer [get-models-handler]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.resources.utils.request :refer [query-params]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [ring.util.response :as response]
   [schema.core :as s]))

(defn merge-by-id [v1 v2 key]
  (vals
   (reduce (fn [acc item]
             (update acc (key item) merge item))
           {}
           (concat v1 v2))))

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
            :summary "Fetch Items with model info [v0]"
            :conflicting true
            :accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :middleware [accept-json-middleware]
            :swagger {:produces ["application/json"]}
            :parameters {:path {:pool_id s/Uuid}
                         :query {(s/optional-key :page) s/Int
                                 (s/optional-key :size) s/Int

                                 (s/optional-key :search_term) s/Str
                                 :result_type (s/enum "Normal" "Min")}}

            :handler (fn [request]
                       (let [result-type (get-in request [:parameters :query :result_type])
                             updated-request (update request :parameters
                                                     #(update % :query merge
                                                              {:not_packaged true :packages false :retired false :result_type "Distinct"}))
                             items-res (get-items-handler updated-request true)]

                         (let [result (if (empty? items-res)
                                        []
                                        (let [ids (mapv :model_id items-res)
                                              models-request (assoc-in updated-request [:parameters :query]
                                                                       {:paginate false :filter_ids ids})
                                              models-res (->> (get-models-handler models-request false)
                                                              (map #(select-keys % [:id :product :manufacturer])))
                                              models-res (rename-key models-res :id :model_id)
                                              merged-result (merge-by-id items-res models-res :model_id)
                                              reduced-res (map #(select-keys % [:inventory_code :product]) merged-result)]

                                          (if (= "Normal" result-type) merged-result reduced-res)))]

                           (-> (response/response result)
                               (response/header "Count" (str (count result)))))))

            :responses {200 {:description "OK"
                             :body [{:inventory_code s/Str
                                     :product s/Str
                                     (s/optional-key :is_package) s/Bool
                                     (s/optional-key :retired) s/Any
                                     (s/optional-key :model_id) s/Uuid
                                     (s/optional-key :parent_id) (s/maybe s/Uuid)
                                     (s/optional-key :id) s/Uuid
                                     (s/optional-key :inventory_pool_id) s/Uuid
                                     (s/optional-key :manufacturer) (s/maybe s/Str)}]}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]

    ["/items"
     {:get {:description "https://staging.leihs.zhdk.ch/manage/8bd16d45-056d-5590-bc7f-12849f034351/items"
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
                                 :result_type (s/enum "Min" "Normal" "Distinct")}}
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
