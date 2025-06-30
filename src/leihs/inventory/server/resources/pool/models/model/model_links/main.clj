(ns leihs.inventory.server.resources.pool.models.model.model-links.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [path-params]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response
                                                    fetch-pagination-params]]
   [ring.util.response :refer [response]]
   [taoensso.timbre :as timbre :refer [debug spy]]))

(defn get-model-links-handler
  ([request]
   (get-model-links-handler request false))
  ([request with-pagination?]
   (let [tx (:tx request)
         {:keys [model_id model_link_id]} (path-params request)
         {:keys [page size]} (fetch-pagination-params request)
         base-query (-> (sql/select :model_links.*)
                        (sql/from :model_links)
                        (sql/where [:= :model_links.model_id model_id])
                        (cond-> model_link_id
                          (sql/where [:= :model_links.id model_link_id])))]
     (create-pagination-response request base-query with-pagination?))))

(defn get-model-links-with-pagination-handler [request]
  (response (get-model-links-handler request true)))