(ns leihs.inventory.server.resources.pool.models.model.images.image.main
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.middlewares.debug :refer [log-by-severity]]
   [leihs.inventory.server.middlewares.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.resources.pool.models.model.images.image.common :refer [handle-image-response]]
   [leihs.inventory.server.utils.request :refer [path-params]]
   [leihs.inventory.server.utils.transform :refer [to-uuid]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response]]))

(def GET_IMAGE_ERROR "Failed to retrieve image")

(defn get-resource [request]
  (try
    (let [tx (:tx request)
          image-id (-> request path-params :image_id)
          query (-> (sql/select :i.*)
                    (sql/from [:images :i])
                    (cond-> image-id
                      (sql/where [:or
                                  [:= :i.id image-id]
                                  [:= :i.parent_id image-id]]))
                    (sql/where [:= :i.thumbnail false])
                    sql-format)
          result (jdbc/execute-one! tx query)]
      (handle-image-response request result))
    (catch Exception e
      (log-by-severity GET_IMAGE_ERROR e)
      (exception-handler request GET_IMAGE_ERROR e))))

(defn delete-resource
  [req]
  (let [tx (:tx req)
        {:keys [image_id]} (:path (:parameters req))
        id (to-uuid image_id)
        res (jdbc/execute-one! tx
                               (sql-format
                                {:delete-from :images :where [:= :id id]}))]
    (if (= (:next.jdbc/update-count res) 1)
      (response {:status "ok" :image_id image_id})
      (bad-request {:message "Failed to delete image"}))))
