(ns leihs.inventory.server.resources.pool.models.model.properties.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [path-params]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response
                                                    fetch-pagination-params]]
   [ring.util.response :refer [response]]
   [taoensso.timbre :as timbre :refer [debug spy]]))

(defn get-properties-handler
  ([request]
   (get-properties-handler request false))
  ([request with-pagination?]
   (let [tx (:tx request)
         {:keys [model_id property_id]} (path-params request)
         {:keys [page size]} (fetch-pagination-params request)
         base-query (-> (sql/select :properties.*)
                      (sql/from :properties)
                      (sql/where [:= :properties.model_id model_id])
                      (cond-> property_id
                        (sql/where [:= :properties.id property_id])))]
     (create-pagination-response request base-query with-pagination?))))

(defn get-properties-with-pagination-handler [request]
  (response (get-properties-handler request true)))