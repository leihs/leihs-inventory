(ns leihs.inventory.server.resources.pool.models.model.items.item.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [path-params]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response
                                                    fetch-pagination-params]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [response status]]
   [taoensso.timbre :as timbre :refer [debug spy]]))

(defn get-resource ([request]
                    (let [tx (:tx request)
                          {:keys [pool_id model_id item_id]} (path-params request)
                          {:keys [page size]} (fetch-pagination-params request)
                          base-query (-> (sql/select :items.*)
                                         (sql/from :items)
                                         (sql/where [:or
                                                     [:= :items.inventory_pool_id pool_id]
                                                     [:= :items.owner_id pool_id]])
                                         (sql/where [:= :items.model_id model_id])
                                         (cond-> item_id
                                           (sql/where [:= :items.id item_id])))
                          result (jdbc/execute-one! tx (-> base-query sql-format))]
                      (if result
                        (response result)
                        (status
                         (response {:status "failure" :message "No entry found"}) 404)))))
