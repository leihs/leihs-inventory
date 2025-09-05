(ns leihs.inventory.server.resources.pool.models.model.images.image.thumbnail.main
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.models.model.images.image.common :refer [handle-image-response]]
   [leihs.inventory.server.resources.pool.models.model.images.image.constants :refer [CONTENT_NEGOTIATION_HEADER_TYPE]]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.helper :refer [log-by-severity]]
   [leihs.inventory.server.utils.request-utils :refer [path-params]]
   [next.jdbc :as jdbc]))

(def GET_THUMBNAIL_ERROR "Failed to retrieve thumbnail")

(defn get-resource [request]
  (try
    (let [tx (:tx request)
          accept-header (get-in request [:headers "accept"])
          json-request? (= accept-header "application/json")
          image_id (-> request path-params :image_id)
          content-negotiation? (= accept-header CONTENT_NEGOTIATION_HEADER_TYPE)

          query (-> (sql/select :i.*)
                    (sql/from [:images :i])
                    (sql/where [:= :i.thumbnail true])
                    (cond-> image_id
                      (sql/where [:or [:= :i.id image_id] [:= :i.parent_id image_id]]))
                    sql-format)
          result (jdbc/execute-one! tx query)]

      (handle-image-response result json-request? content-negotiation? accept-header))
    (catch Exception e
      (log-by-severity GET_THUMBNAIL_ERROR e)
      (exception-handler GET_THUMBNAIL_ERROR e))))
