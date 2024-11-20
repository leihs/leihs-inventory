(ns leihs.inventory.server.resources.models.form.software.model-by-pool-form-fetch
  (:require
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [honey.sql :as sq :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.models.queries :refer [attachments-query]]
   [leihs.inventory.server.resources.utils.request :refer [path-params]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.pagination :refer [pagination-response]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response]]
   [taoensso.timbre :refer [error]])
  (:import [java.time LocalDateTime]))

(defn select-entries [tx table columns where-clause]
  (jdbc/execute! tx
                 (-> (apply sql/select columns)
                     (sql/from table)
                     (sql/where where-clause)
                     sql-format)))

(defn fetch-attachments [tx model-id]
  (select-entries tx :attachments [:id :filename :content_type] [:= :model_id model-id]))

(defn create-software-handler-by-pool-form-fetch [request]
  (let [tx (get-in request [:tx])
        model-id (to-uuid (get-in request [:path-params :model_id]))
        pool-id (to-uuid (get-in request [:path-params :pool_id]))]
    (try
      (let [model-query (-> (sql/select :m.id :m.product :m.manufacturer :m.version :m.type
                                        :m.hand_over_note :m.description :m.internal_description
                                        :m.technical_detail :m.is_package)
                            (sql/from [:models :m])
                            (sql/where [:and [:= :m.id model-id] [:= :m.type "Software"]])
                            sql-format)
            model-result (jdbc/execute-one! tx model-query)
            attachments (fetch-attachments tx model-id)
            result (if model-result
                     [(assoc model-result :attachments attachments)]
                     [])]
        (if result
          (response result)
          (bad-request {:error "Failed to fetch model"})))
      (catch Exception e
        (error "Failed to fetch model" (.getMessage e))
        (bad-request {:error "Failed to fetch model" :details (.getMessage e)})))))
