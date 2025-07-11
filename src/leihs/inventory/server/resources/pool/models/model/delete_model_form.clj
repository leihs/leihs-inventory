(ns leihs.inventory.server.resources.pool.models.model.delete-model-form
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [next.jdbc :as jdbc]
   [leihs.inventory.server.resources.pool.models.common :refer [apply-cover-image-urls  fetch-thumbnails-for-ids
                                                                remove-nil-values]]
   [ring.util.response :refer [response]]
   [taoensso.timbre :refer [error]])
  (:import [java.util UUID]))

(defn db-operation
  "Executes a SELECT or DELETE operation on the given table based on the operation keyword using next.jdbc and HoneySQL."
  [tx operation table where-clause]
  (let [query (case operation
                :select
                (-> (sql/select :*)
                    (sql/from (keyword table))
                    (sql/where where-clause)
                    sql-format)
                :delete (-> (sql/delete-from table)
                            (sql/where where-clause)
                            sql-format)
                (throw (IllegalArgumentException. "Unsupported operation")))]
    (jdbc/execute! tx query)))

(defn filter-keys
  "Filters the keys of each map in the vector, keeping only the specified keys."
  [vec-of-maps keys-to-keep]
  (mapv #(select-keys % keys-to-keep) vec-of-maps))

(defn delete-resource [request]
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
                                     (-> (sql/delete-from :models)
                                         (sql/where [:= :id model-id])
                                         (sql/returning :*)
                                         sql-format))
        _ (db-operation tx :delete :images [:= :target_id model-id])

        remaining-attachments (db-operation tx :select :attachments [:= :model_id model-id])
        remaining-images (db-operation tx :select :images [:= :target_id model-id])
        _ (when (or (seq remaining-attachments) (seq remaining-images))
            (throw (ex-info "Referenced attachments or images still exist" {:status 403})))

        result {:deleted_attachments (remove-nil-values (filter-keys attachments [:id :model_id :filename :size]))
                :deleted_images (remove-nil-values (filter-keys images [:id :target_id :filename :size :thumbnail]))
                :deleted_model (remove-nil-values (filter-keys deleted-model [:id :product :manufacturer]))}]

    (if (= 1 (count deleted-model))
      (response result)
      (throw (ex-info "Failed to delete model" {:status 403})))))