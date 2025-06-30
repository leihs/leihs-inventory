(ns leihs.inventory.server.resources.pool.models.model.entitlements.main
  (:require
   [clojure.set]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [path-params]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response
                                                    fetch-pagination-params]]
   [ring.util.response :refer [response]]))

(defn get-entitlements-handler
  ([request]
   (get-entitlements-handler request false))
  ([request with-pagination?]
   (let [tx (:tx request)
         {:keys [pool_id model_id entitlement_id]} (path-params request)
         {:keys [page size]} (fetch-pagination-params request)
         base-query (-> (sql/select :entitlements.*)
                        (sql/from :entitlements)
                        (sql/join :entitlement_groups
                                  [:= :entitlements.entitlement_group_id :entitlement_groups.id])
                        (sql/where [:= :entitlement_groups.inventory_pool_id pool_id])
                        (sql/where [:= :entitlements.model_id model_id])
                        (cond-> entitlement_id
                          (sql/where [:= :entitlements.id entitlement_id])))]
     (create-pagination-response request base-query with-pagination?))))

(defn get-entitlements-with-pagination-handler [request]
  (response (get-entitlements-handler request true)))