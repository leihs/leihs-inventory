(ns leihs.inventory.server.resources.models.accessories.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.models.queries :refer [accessories-query attachments-query base-pool-query
                                                            entitlements-query item-query
                                                            model-links-query properties-query]]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.core :refer [single-entity-get-request?]]
   [leihs.inventory.server.utils.helper :refer [convert-map-if-exist]]
   [leihs.inventory.server.utils.pagination :refer [fetch-pagination-params pagination-response create-pagination-response]]
   [next.jdbc.sql :as jdbc]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [debug error spy]]))

(defn base-query [model-id]
  (-> (sql/select :accessories.*)
      (sql/from :accessories)
      (sql/where [:= :accessories.model_id model-id])))

(defn get-accessory-handler [request]
  (debug "get-accessory-handler")
  (let [tx (:tx request)
        {:keys [model_id accessory_id]} (path-params request)]
    (-> (base-query model_id)
        (sql/where [:= :accessories.id accessory_id])
        sql-format
        (->> (jdbc/query tx))
        first
        spy)))

(defn get-multiple-accessories-handler
  ([request]
   (get-multiple-accessories-handler request false))
  ([request with-pagination?]
   (let [tx (:tx request)
         {:keys [model_id]} (path-params request)
         query (base-query model_id)]
     (create-pagination-response request query with-pagination?))))

(defn get-accessories-with-pagination-handler [request]
  (response (get-multiple-accessories-handler request true)))
