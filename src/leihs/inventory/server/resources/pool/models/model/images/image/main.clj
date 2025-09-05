(ns leihs.inventory.server.resources.pool.models.model.images.image.main
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.models.model.images.image.common :refer [handle-image-response]]
   [leihs.inventory.server.resources.pool.models.model.images.image.constants :refer [CONTENT_NEGOTIATION_HEADER_TYPE]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.helper :refer [log-by-severity]]
   [leihs.inventory.server.utils.request-utils :refer [path-params]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response]]))

(def GET_IMAGE_ERROR "Failed to retrieve image")

(defn get-resource [request]
  (try
    (let [tx (:tx request)
          accept-header (get-in request [:headers "accept"])
          json-request? (= accept-header "application/json")
          image_id (-> request path-params :image_id)
          content-negotiation? (= accept-header CONTENT_NEGOTIATION_HEADER_TYPE)
          query (-> (sql/select :i.*)
                    (sql/from [:images :i])
                    (cond-> image_id
                      (sql/where [:or [:= :i.id image_id] [:= :i.parent_id image_id]]))
                    (sql/where [:= :i.thumbnail false])
                    sql-format)
          result (jdbc/execute-one! tx query)]

      (handle-image-response result json-request? content-negotiation? accept-header))
    (catch Exception e
      (log-by-severity GET_IMAGE_ERROR e)
      (exception-handler GET_IMAGE_ERROR e))))

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
