(ns leihs.inventory.server.resources.pool.models.model.items.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response
                                                    fetch-pagination-params]]
   [leihs.inventory.server.utils.request-utils :refer [path-params]]
   [ring.util.response :refer [response]]
   [taoensso.timbre :as timbre :refer [debug]]))

(defn get-items-handler
  ([request]
   (get-items-handler request false))
  ([request with-pagination?]
   (let [tx (:tx request)
         {:keys [pool_id model_id item_id]} (path-params request)
         {:keys [page size]} (fetch-pagination-params request)
         base-query (-> (sql/select :items.*)
                        (sql/from :items)
                        (sql/where [:or
                                    [:= :items.inventory_pool_id pool_id]
                                    [:= :items.owner_id pool_id]])
                        (sql/where [:= :items.model_id model_id]))]
     (debug (sql-format base-query :inline true))
     (create-pagination-response request base-query with-pagination?))))

(defn index-resources [request]
  (response (get-items-handler request true)))