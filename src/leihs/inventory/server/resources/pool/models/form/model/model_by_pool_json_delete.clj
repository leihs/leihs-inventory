(ns leihs.inventory.server.resources.pool.models.form.model.model-by-pool-json-delete
  (:require
   [honey.sql :as sql]
   [honey.sql.helpers :as sql-helpers]
   [leihs.inventory.server.resources.pool.models.form.common :refer [filter-keys db-operation]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [response]]
   [taoensso.timbre :refer [error]])
  (:import [java.util UUID]))

(defn delete-model-handler-by-pool-json [request]
  (let [model-id (to-uuid (get-in request [:path-params :model_id]))
        tx (:tx request)
        models (db-operation tx :select :models [:= :id model-id])
        _ (when-not (seq models)
            (throw (ex-info "Model not found" {:status 404})))

        items (db-operation tx :select :items [:= :model_id model-id])
        attachments (db-operation tx :select :attachments [:= :model_id model-id])
        images (db-operation tx :select :images [:= :target_id model-id])
        _ (when (seq items)
            (throw (ex-info "Referenced items exist" {:status 403})))

        deleted-model (jdbc/execute! tx
                                     (-> (sql-helpers/delete-from :models)
                                         (sql-helpers/where [:= :id model-id])
                                         (sql-helpers/returning :*)
                                         sql/format))
        _ (db-operation tx :delete :images [:= :target_id model-id])

        remaining-attachments (db-operation tx :select :attachments [:= :model_id model-id])
        remaining-images (db-operation tx :select :images [:= :target_id model-id])
        _ (when (or (seq remaining-attachments) (seq remaining-images))
            (throw (ex-info "Referenced attachments or images still exist" {:status 403})))

        result {:deleted_attachments (filter-keys attachments [:id :model_id :filename :size])
                :deleted_images (filter-keys images [:id :target_id :filename :size :thumbnail])
                :deleted_model (filter-keys deleted-model [:id :product :manufacturer])}]

    (if (= 1 (count deleted-model))
      (response result)
      (throw (ex-info "Failed to delete model" {:status 403})))))