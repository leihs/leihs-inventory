(ns leihs.inventory.server.resources.pool.models-compatibles.main
  (:require
   [clojure.set]
   [clojure.string :as str]
   [honey.sql :refer [format]
    :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.common :refer [create-image-url remove-nil-entries-fnc]]
   [leihs.inventory.server.resources.utils.request :refer [path-params]]
   [leihs.inventory.server.utils.pagination :refer [create-paginated-response fetch-pagination-params-raw]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [debug error spy]]))

(defn get-models-compatible-handler [request]
  (try
    (let [tx (:tx request)
          model_id (-> request path-params :model_id)
          base-query (-> (sql/select [:models.id :model_id]
                                     :models.product
                                     :models.version
                                     (create-image-url :models :cover_image_url)
                                     :models.cover_image_id)
                         (sql/from :models)
                         (sql/left-join :images [:= :models.cover_image_id :images.id])
                         (cond-> model_id
                           (-> (sql/join :models_compatibles [:= :models_compatibles.model_id model_id])
                               (sql/where [:= :models.id model_id])))
                         (sql/order-by [[:trim [:|| :models.product " " :models.version]] :asc]))
          {:keys [page size]} (fetch-pagination-params-raw request)]
      (if (or model_id (and (nil? page) (nil? size)))
        (-> (jdbc/execute! tx (-> base-query sql-format))
            remove-nil-entries-fnc
            response)
        (response (create-paginated-response base-query tx size page remove-nil-entries-fnc))))
    (catch Exception e
      (error "Failed to get models-compatible" e)
      (bad-request {:error "Failed to get models-compatible" :details (.getMessage e)}))))
