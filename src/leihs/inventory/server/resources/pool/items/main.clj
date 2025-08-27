(ns leihs.inventory.server.resources.pool.items.main
  (:require
   [clojure.set]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [leihs.inventory.server.utils.request-utils :refer [path-params
                                                       query-params]]
   [ring.middleware.accept]
   [ring.util.response :refer [response]]))

(defn base-pool-query [query pool-id]
  (-> query
      (sql/from [:items :i])
      (sql/join [:rooms :r] [:= :r.id :i.room_id])
      (sql/join [:models :m] [:= :m.id :i.model_id])
      (sql/join [:buildings :b] [:= :b.id :r.building_id])
      (cond->
       pool-id (sql/join [:inventory_pools :ip] [:= :ip.id :i.inventory_pool_id])
       pool-id (sql/where [:= :ip.id [:cast pool-id :uuid]]))))

(defn base-pool-query-distinct [query pool-id]
  (-> query
      (sql/from [:items :i])
      (sql/join [:models :m] [:= :m.id :i.model_id])
      (cond-> pool-id (sql/where [:= :i.inventory_pool_id [:cast pool-id :uuid]]))
      (sql/group-by :m.product :i.model_id :i.inventory_code :i.inventory_pool_id :i.retired :m.is_package :i.id :i.parent_id)))

(defn index-resources
  ([request]
   (let [{:keys [pool_id item_id]} (path-params request)
         {:keys [search_term not_packaged packages retired result_type]} (query-params request)

         base-select (cond
                       (= result_type "Distinct") (sql/select-distinct-on [:m.product]
                                                                          :i.retired :i.parent_id :i.id
                                                                          :m.is_package
                                                                          :i.inventory_code
                                                                          :i.model_id
                                                                          :i.inventory_pool_id
                                                                          :m.product)
                       (= result_type "Min") (sql/select :i.retired :i.parent_id :i.id :i.inventory_code :i.model_id :m.is_package)
                       :else (sql/select :m.is_package :i.* [:b.name :building_name] [:r.name :room_name]))

         base-query (-> base-select
                        ((fn [query]
                           (if (= result_type "Distinct")
                             (base-pool-query-distinct query pool_id)
                             (base-pool-query query pool_id))))

                        (cond-> item_id (sql/where [:= :i.id item_id]))

                        (cond-> (= true retired) (sql/where [:is-not :i.retired nil]))
                        (cond-> (= false retired) (sql/where [:is :i.retired nil]))

                        (cond-> (= true packages) (sql/where [:= :m.is_package true]))
                        (cond-> (= false packages) (sql/where [:= :m.is_package false]))

                        (cond-> (= true not_packaged) (sql/where [:is :i.parent_id nil]))
                        (cond-> (= false not_packaged) (sql/where [:is-not :i.parent_id nil]))

                        (cond-> (seq search_term)
                          (sql/where [:or [:ilike :i.inventory_code (str "%" search_term "%")] [:ilike :m.product (str "%" search_term "%")]
                                      [:ilike :m.manufacturer (str "%" search_term "%")]]))

                        (cond-> item_id (sql/where [:= :i.id item_id]))

                        (cond-> (and sort-by item_id) (sql/order-by item_id)))]

     (response (create-pagination-response request base-query nil)))))
