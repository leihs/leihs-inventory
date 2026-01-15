(ns leihs.inventory.server.resources.pool.software.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.middlewares.debug :refer [log-by-severity]]
   [leihs.inventory.server.middlewares.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.resources.pool.common :refer [fetch-attachments
                                                         str-to-bool]]
   [leihs.inventory.server.resources.pool.models.common :refer [fetch-thumbnails-for-ids
                                                                filter-map-by-spec
                                                                model->enrich-with-image-attr]]
   [leihs.inventory.server.resources.pool.models.helper :refer [normalize-model-data]]
   [leihs.inventory.server.resources.pool.software.types :as types]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [leihs.inventory.server.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.transform :refer [to-uuid]]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]
   [ring.util.response :refer [bad-request response]])
  (:import
   (java.time LocalDateTime)))

(def ERROR_CREATE_SOFTWARE "Failed to create software")
(def ERROR_GET_SOFTWARE "Failed to get software")

(defn prepare-software-data
  [data]
  (let [normalize-data (normalize-model-data data)
        created-ts (LocalDateTime/now)]
    (assoc normalize-data
           :type "Software"
           :created_at created-ts
           :updated_at created-ts)))

(def base-query
  (-> (sql/select :models.id
                  :models.product :models.version :models.name
                  :models.cover_image_id)
      (sql/from :models)
      (sql/where [:= :models.type "Software"])
      (sql/order-by :models.name)))

(defn get-by-id [tx id]
  (-> base-query
      (sql/where [:= :models.id id])
      sql-format
      (->> (jdbc-query tx))
      first))

(defn index-resources [request]
  (try
    (let [tx (:tx request)
          pool-id (-> request path-params :pool_id)
          {:keys [search search_term]} (query-params request)
          term (or search search_term) ; search_term needed for fields
          base-query (-> base-query
                         (cond-> term
                           (sql/where [:ilike :models.name (str "%" term "%")])))
          post-fnc (fn [models]
                     (->> models
                          (fetch-thumbnails-for-ids tx)
                          (map (model->enrich-with-image-attr pool-id))))]

      (response (create-pagination-response request base-query nil post-fnc)))

    (catch Exception e
      (log-by-severity ERROR_GET_SOFTWARE e)
      (exception-handler request ERROR_GET_SOFTWARE e))))

(defn post-resource [request]
  (let [tx (:tx request)
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        multipart (get-in request [:parameters :body])
        prepared-model-data (-> (prepare-software-data multipart)
                                (assoc :is_package (str-to-bool (:is_package multipart))))]

    (try
      (let [res (jdbc/execute-one! tx (-> (sql/insert-into :models)
                                          (sql/values [prepared-model-data])
                                          (sql/returning :*)
                                          sql-format))
            model-id (:id res)
            res (when res (let [attachments (fetch-attachments tx model-id pool-id)
                                result (assoc res :attachments attachments)] result))]

        (if res
          (response (filter-map-by-spec res ::types/post-response))
          (bad-request {:message "Failed to create software"})))
      (catch Exception e
        (log-by-severity ERROR_CREATE_SOFTWARE e)
        (exception-handler request ERROR_CREATE_SOFTWARE e)))))
