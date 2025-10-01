(ns leihs.inventory.server.resources.pool.items.main
  (:require
   [clojure.set]

   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]

   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [leihs.inventory.server.utils.request-utils :refer [pick-fields
                                                       path-params
                                                       query-params]]
   [next.jdbc :as jdbc]
   [ring.middleware.accept]

   [ring.util.response :refer [response]]
   [taoensso.timbre :refer [debug]]))

(defn in-stock [query true-or-false]
  (-> query
      (sql/where [:= :i.parent_id nil])
      (sql/where
       [(if true-or-false :not-exists :exists)
        (-> (sql/select 1)
            (sql/from :reservations)
            (sql/where [:= :reservations.item_id :i.id])
            (sql/where [:and
                        [:= :reservations.status ["signed"]]
                        [:= :reservations.returned_date nil]]))])))

(defn owner-or-responsible-cond [pool-id]
  [:or
   [:= :i.owner_id pool-id]
   [:= :i.inventory_pool_id pool-id]])

(defn owner-and-responsible-cond [pool-id inventory-pool-id]
  [:and
   [:= :i.owner_id pool-id]
   [:= :i.inventory_pool_id inventory-pool-id]])

(defn not-owner-and-responsible-cond [pool-id inventory-pool-id]
  [:and
   [:not= :i.owner_id pool-id]
   [:= :i.inventory_pool_id inventory-pool-id]])

(defn index-resources
  ([request]
   (let [{:keys [pool_id]} (path-params request)
         {:keys [fields search_term
                 model_id parent_id
                 retired borrowable
                 incomplete broken owned
                 inventory_pool_id
                 in_stock before_last_check]} (query-params request)

         query-params (fn [query]
                        (-> query
                            (#(cond
                                (and inventory_pool_id (true? owned))
                                (sql/where % (owner-and-responsible-cond pool_id inventory_pool_id))

                                (and inventory_pool_id (false? owned))
                                (sql/where % (not-owner-and-responsible-cond pool_id inventory_pool_id))

                                inventory_pool_id
                                (sql/where % (owner-or-responsible-cond inventory_pool_id))

                                (true? owned)
                                (sql/where % [:= :i.owner_id pool_id])

                                (false? owned)
                                (sql/where % [:not= :i.owner_id pool_id])

                                :else %))
                            (cond-> (boolean? in_stock) (in-stock in_stock))
                            (cond-> before_last_check
                              (sql/where [:<= :i.last_check before_last_check]))
                            (cond-> (boolean? retired)
                              (sql/where [(if retired :<> :=) :i.retired nil]))
                            (cond-> (boolean? borrowable)
                              (sql/where [:= :i.is_borrowable borrowable]))
                            (cond-> (boolean? broken)
                              (sql/where [:= :i.is_broken broken]))
                            (cond-> (boolean? incomplete)
                              (sql/where [:= :i.is_incomplete incomplete]))))

         select (sql/select
                 :i.*
                 [:ip.name :inventory_pool_name]
                 [:r.end_date :reservation_end_date]
                 [:r.user_id :reservation_user_id]
                 [:m.is_package :is_package]
                 [:m.name :model_name]
                 [:rs.name :room_name]
                 [:rs.description :room_description]
                 [:b.name :building_name]
                 [:b.code :building_code]

                 [[:nullif [:concat_ws " " :u.firstname :u.lastname] ""] :reservation_user_name]

                 [(-> (sql/select :%count.*) ; [[:count :*]]
                      (sql/from :items)
                      (sql/where [:and
                                  [:= :items.parent_id :i.id]]))
                  :package_items])

         query (-> select
                   (sql/from [:items :i])
                   ;; Join inventory pool
                   (sql/join [:inventory_pools :ip]
                             [:= :ip.id :i.inventory_pool_id])

                   ;; Join rooms
                   (sql/join [:rooms :rs]
                             [:= :i.room_id :rs.id])

                   ;; Join rooms
                   (sql/join [:buildings :b]
                             [:= :rs.building_id :b.id])

                   ;; Join models
                   (sql/left-join [:models :m]
                                  [:or
                                   [:= :i.model_id :m.id]
                                   [:= :m.id model_id]])

                   ;; Join reservations (only active)
                   (sql/left-join [:reservations :r]
                                  [:and
                                   [:= :r.item_id :i.id]
                                   [:= :r.returned_date nil]])

                    ;; Join users
                   (sql/left-join [:users :u]
                                  [:= :u.id :r.user_id])

                    ;; Filters
                   (sql/where [:or
                               [:= :i.inventory_pool_id pool_id]
                               [:= :i.owner_id pool_id]])

                   (cond-> model_id (sql/where [:= :i.model_id model_id]))
                   (cond-> parent_id (sql/where [:= :i.parent_id parent_id]))

                   query-params

                   (cond-> (seq search_term)
                     (sql/where [:or
                                 [:ilike :i.inventory_code (str "%" search_term "%")]
                                 [:ilike :m.product (str "%" search_term "%")]
                                 [:ilike :m.manufacturer (str "%" search_term "%")]])))]

     (debug (sql-format query :inline true))
     (-> request
         (create-pagination-response query nil)
         (pick-fields fields)
         response))))
