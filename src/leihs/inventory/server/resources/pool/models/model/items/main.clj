(ns leihs.inventory.server.resources.pool.models.model.items.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [leihs.inventory.server.utils.request-utils :refer [path-params]]
   [ring.util.response :refer [response]]
   [taoensso.timbre :as timbre :refer [debug]]))

(defn index-resources [request]
  (let [{:keys [pool_id model_id]} (path-params request)
        base-query

        (-> (sql/select
             :i.*
             [:ip.name :inventory_pool_name]
             [:r.end_date :reservation_end_date]
             [:r.user_id :reservation_user_id]
             [:m.is_package :is_package]
             [[:nullif [:concat_ws " " :u.firstname :u.lastname] ""] :reservation_user_name]

             [(-> (sql/select :%count.*) ; [[:count :*]]
                  (sql/from :items)
                  (sql/where [:and
                              [:= :items.parent_id :i.id]]))
              :package_items_count])

            (sql/from [:items :i])

            ;; Join inventory pool
            (sql/join [:inventory_pools :ip]
                      [:= :ip.id :i.inventory_pool_id])

            ;; Join models
            (sql/join [:models :m]
                      [:= :m.id model_id])

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
            (sql/where [:= :i.model_id model_id])
            (sql/order-by :i.inventory_code))]

    (debug (sql-format base-query :inline true))
    (response (create-pagination-response request base-query nil))))

