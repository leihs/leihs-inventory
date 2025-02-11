(ns leihs.inventory.server.resources.models.form.package.model-by-pool-form-update
  (:require
   [cheshire.core :as cjson]
   [cheshire.core :as jsonc]
   [cheshire.core :refer [generate-string] :rename {generate-string to-json}]
   [clojure.data.codec.base64 :as b64]
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.models.form.license.common :refer [remove-nil-entries cast-to-uuid-or-nil double-to-numeric-or-nil parse-local-date-or-nil calculate-retired-value remove-empty-or-nil remove-entries-by-keys]]
   [leihs.inventory.server.resources.models.form.package.model-by-pool-form-create :refer [prepare-package-data split-items]]
   [leihs.inventory.server.resources.models.helper :refer [str-to-bool normalize-model-data normalize-files
                                                           process-attachments parse-json-map parse-json-array]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response]]
   [taoensso.timbre :refer [error]])
  (:import [java.time LocalDateTime]))

(defn create-validation-response [data validation]
  {:data data :validation validation})

(defn update-item-handler [{item-id :item_id model-id :model_id pool-id :pool_id tx :tx request :request item-entry :item-entry}]
  (let [created-ts (LocalDateTime/now)
        tx (:tx request)
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        multipart (get-in request [:parameters :multipart])
        items_attributes (parse-json-array request :items_attributes)
        multipart (assoc multipart :inventory_pool_id pool-id)

          ;; FIXME: handle retired_reason with NEW-FE
        multipart (dissoc multipart :retired)

        prepared-package-data (prepare-package-data multipart)
        split-items (split-items items_attributes)]
    (try
      (let [update-model-query (-> (sql/update [:items :i])
                                   (sql/set prepared-package-data)
                                 ;(sql/join [:models :m] [:= :i.model_id :m.id])
                                 ;  (sql/where [:= :i.id item-id] )
                                   (sql/where [:and [:= :i.model_id model-id] [:= :i.id item-id]])
                                   (sql/returning :*)
                                   sql-format)
            res (jdbc/execute-one! tx update-model-query)

            ;; Link items from package
            link-res (let [ids-to-link (get split-items :ids-to-link)]
                       (when (seq ids-to-link)
                         (let [update-link-items-query (-> (sql/update :items)
                                                           (sql/set {:parent_id (:id res)})
                                                           (sql/where [:in :id ids-to-link])
                                                           (sql/where [:is :parent_id nil])
                                                           (sql/returning :*)
                                                           sql-format)
                               linked-items-res (jdbc/execute! tx update-link-items-query)]
                           linked-items-res)))

            ;; Unlink items from package
            unlink-res (let [ids-to-unlink (get split-items :ids-to-unlink)]
                         (when (seq ids-to-unlink)
                           (let [update-unlink-items-query (-> (sql/update :items)
                                                               (sql/set {:parent_id nil})
                                                               (sql/where [:in :id ids-to-unlink])
                                                               (sql/where [:is-not :parent_id nil])
                                                               (sql/returning :*)
                                                               sql-format)
                                 unlinked-items-res (jdbc/execute! tx update-unlink-items-query)]
                             unlinked-items-res)))]
        (if res
          (response (create-validation-response res []))
          (bad-request {:error "Failed to update item"})))
      (catch Exception e
        (error "Failed to update item" (.getMessage e))
        (bad-request {:error "Failed to update item" :details (.getMessage e)})))))

(defn fetch-license-data [tx model-id item-id pool-id]
  (let [query (-> (sql/select :*)
                  (sql/from :items)
                  (sql/where [:= :id item-id] [:= :model_id model-id] [:= :inventory_pool_id pool-id])
                  sql-format)
        res (jdbc/execute-one! tx query)]
    res))

(defn update-package-handler-by-pool-form [request]
  (let [item-id (to-uuid (get-in request [:path-params :item_id]))
        model-id (to-uuid (get-in request [:path-params :model_id]))
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        tx (:tx request)
        res (fetch-license-data tx model-id item-id pool-id)]
    (if res
      (update-item-handler {:item_id item-id :model_id model-id :pool_id pool-id :tx tx :request request :item-entry res})
      (bad-request {:error "Failed to update item" :details "No data found"}))))