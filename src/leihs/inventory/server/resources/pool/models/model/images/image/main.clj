(ns leihs.inventory.server.resources.pool.models.model.images.image.main
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.models.model.images.image.common :refer [convert-base64-to-byte-stream]]
   [leihs.inventory.server.resources.pool.models.model.images.image.constants :refer [CONTENT_NEGOTIATION_HEADER_TYPE]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.helper :refer [log-by-severity]]
   [leihs.inventory.server.utils.request-utils :refer [path-params]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response status]]))

(def GET_IMAGE_ERROR "Failed to retrieve image")

(defn get-resource [request]
  (try
    (let [tx (:tx request)
          accept-header (get-in request [:headers "accept"])
          json-request? (= accept-header "application/json")
          content-negotiation? (= accept-header CONTENT_NEGOTIATION_HEADER_TYPE)
          image_id (-> request path-params :image_id)
          query (-> (sql/select :i.*)
                    (sql/from [:images :i])
                    (cond-> image_id
                      (sql/where [:or [:= :i.id image_id] [:= :i.parent_id image_id]]))
                    (sql/where [:= :i.thumbnail false])
                    (cond-> (and (not json-request?) (not content-negotiation?))
                      (sql/where [:= :i.content_type accept-header]))
                  ;; TODO: pool_id / model_id restrictions
                    sql-format)
          result (jdbc/execute-one! tx query)]

      (cond
        (nil? result) (status (response {:status "failure" :message "No image found"}) 404)
        (and json-request? image_id) (response result)
        (and json-request? (nil? image_id)) (response {:data result})
        (and (not json-request?) image_id) (convert-base64-to-byte-stream result)))
    (catch Exception e
      (log-by-severity GET_IMAGE_ERROR e)
      (bad-request {:error GET_IMAGE_ERROR :details (.getMessage e)}))))

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
      (bad-request {:error "Failed to delete image"}))))
