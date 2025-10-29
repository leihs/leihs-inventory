(ns leihs.inventory.server.resources.pool.models.model.images.image.thumbnail.main
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.models.model.images.image.common :refer [handle-image-response]]
   [leihs.inventory.server.utils.debug :refer [log-by-severity]]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.request-utils :refer [path-params]]
   [next.jdbc :as jdbc]))

(def GET_THUMBNAIL_ERROR "Failed to retrieve thumbnail")

(defn get-resource [request]
  (try
    (let [tx (:tx request)
          image-id (-> request path-params :image_id)
          query (-> (sql/select :i.*)
                    (sql/from [:images :i])
                    (sql/where [:= :i.thumbnail true])
                    (cond-> image-id
                      (sql/where [:or
                                  [:= :i.id image-id]
                                  [:= :i.parent_id image-id]]))
                    sql-format)
          result (jdbc/execute-one! tx query)]
      (handle-image-response request result))
    (catch Exception e
      (log-by-severity GET_THUMBNAIL_ERROR e)
      (exception-handler request GET_THUMBNAIL_ERROR e))))
