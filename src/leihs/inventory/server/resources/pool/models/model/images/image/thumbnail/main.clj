(ns leihs.inventory.server.resources.pool.models.model.images.image.thumbnail.main
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.models.model.images.image.common :refer [convert-base64-to-byte-stream]]
   [leihs.inventory.server.resources.pool.models.model.images.image.constants :refer [CONTENT_NEGOTIATION_HEADER_TYPE]]
   [leihs.inventory.server.utils.helper :refer [log-by-severity]]
   [leihs.inventory.server.utils.request-utils :refer [path-params]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response status]]))

(def GET_THUMBNAIL_ERROR "Failed to retrieve thumbnail")

(defn get-resource [request]
  (try
    (let [tx (:tx request)
          accept-header (get-in request [:headers "accept"])
          json-request? (= accept-header "application/json")
          content-negotiation? (= accept-header CONTENT_NEGOTIATION_HEADER_TYPE)
          image_id (-> request path-params :image_id)

          query (-> (sql/select :i.*)
                    (sql/from [:images :i])
                    (sql/where [:= :i.thumbnail true])
                    (cond-> image_id
                      (sql/where [:or [:= :i.id image_id] [:= :i.parent_id image_id]]))
                    (cond-> (and (not json-request?) (not content-negotiation?))
                      (sql/where [:= :i.content_type accept-header]))
                    sql-format)
          result (jdbc/execute-one! tx query)]

      (cond
        (nil? result) (status (response {:status "failure" :message "No thumbnail found"}) 404)
        (and json-request? image_id) (response result)
        (and json-request? (nil? image_id)) (response {:data result})
        (and (not json-request?) image_id) (convert-base64-to-byte-stream result)))
    (catch Exception e
      (log-by-severity GET_THUMBNAIL_ERROR e)
      (bad-request {:error GET_THUMBNAIL_ERROR :details (.getMessage e)}))))
