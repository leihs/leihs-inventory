(ns leihs.inventory.server.resources.pool.models.model.attachments.main
  (:require
   [cheshire.core :as cjson]
   [clojure.data.codec.base64 :as b64]
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.java.shell :refer [sh]]
   [clojure.set :as set]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.core :refer [presence]]
   [leihs.inventory.server.resources.pool.common :refer [str-to-bool]]
   [leihs.inventory.server.resources.pool.models.helper :refer [normalize-model-data]]
   [leihs.inventory.server.resources.pool.models.queries :refer [accessories-query attachments-query base-inventory-query
                                                                 entitlements-query item-query
                                                                 model-links-query properties-query
                                                                 with-items without-items with-search filter-by-type
                                                                 from-category]]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]

   [leihs.inventory.server.utils.constants :refer [config-get]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.helper :refer [convert-map-if-exist url-ends-with-uuid?]]
   [leihs.inventory.server.utils.image-upload-handler :refer [file-to-base64 resize-and-convert-to-base64]]
   [leihs.inventory.server.utils.pagination :refer [fetch-pagination-params]]
   [leihs.inventory.server.utils.pagination :refer [pagination-response create-pagination-response]]
   [next.jdbc :as jdbc]
   [pantomime.extract :as extract]
   [ring.util.response :as response :refer [bad-request response status]]
   [taoensso.timbre :refer [error spy debug]])
  (:import [java.io File FileInputStream ByteArrayOutputStream]
           [java.net URL JarURLConnection]
           (java.time LocalDateTime)
           [java.util Base64]
           [java.util UUID]
           [java.util.jar JarFile]
           [org.im4java.core ConvertCmd IMOperation]))

;(defn create-image-url [col-name col-name-keyword]
;  [[[:raw (str "CASE WHEN " (name col-name) ".cover_image_id IS NOT NULL THEN CONCAT('/inventory/images/', " (name col-name) ".cover_image_id, '/thumbnail') ELSE NULL END")]]
;   col-name-keyword])

(defn sanitize-filename [filename]
  (str/replace filename #"[^a-zA-Z0-9_.-]" "_"))

(defn filter-keys [m keys-to-keep]
  (select-keys m keys-to-keep))

(defn filter-keys-attachments [m]
  (filter-keys m [:filename :content_type :size :model_id :item_id :content]))

(defn attachment-response-format [s]
  (sql/returning s :id :filename))
<
(defn upload-attachment [req]
  (try
    (let [{{:keys [model_id]} :path} (:parameters req)
          body-stream (:body req)
          allowed-file-types (config-get :api :attachments :allowed-file-types)
          max-size-mb (config-get :api :attachments :max-size-mb)
          upload-path (config-get :api :upload-dir)
          tx (:tx req)
          content-type (get-in req [:headers "content-type"])
          filename-to-save (sanitize-filename (get-in req [:headers "x-filename"]))
          content-length (some-> (get-in req [:headers "content-length"]) Long/parseLong)
          file-full-path (str upload-path filename-to-save)
          entry {:tempfile file-full-path
                 :filename filename-to-save
                 :content_type content-type
                 :size content-length
                 :model_id model_id}]

      (let [allowed-extensions allowed-file-types
            content-extension (last (clojure.string/split content-type #"/"))]
        (when-not (some #(= content-extension %) allowed-extensions)
          (throw (ex-info "Invalid file type" {:status 400 :error "Unsupported file type"}))))

      (when (> content-length (* max-size-mb 1024 1024))
        (throw (ex-info "File size exceeds limit" {:status 400 :error "File size exceeds limit"})))

      (io/copy body-stream (io/file file-full-path))

      (let [file-content (file-to-base64 entry)
            data (-> entry
                     (assoc :content file-content)
                     filter-keys-attachments)
            data (jdbc/execute! tx (-> (sql/insert-into :attachments)
                                       (sql/values [data])
                                       attachment-response-format
                                       sql-format))]
        (status (response data) 200)))

    (catch Exception e
      (error "Failed to upload attachment" e)
      (bad-request {:error "Failed to upload attachment" :details (.getMessage e)}))))

(defn validate-empty-string!
  ([k vec-of-maps]
   (validate-empty-string! k vec-of-maps nil))
  ([k vec-of-maps scope]
   (doseq [m vec-of-maps]
     (when (and (contains? m k) (= "" (get m k)))
       (throw (ex-info (str "Field '" k "' cannot be an empty string.")
                       (merge {:key k :map m} (when scope {:scope scope}))))))))

;; THIS by pool
(defn get-models-handler
  ([request]
   (get-models-handler request false))
  ([request with-pagination?]
   (let [tx (:tx request)
         {:keys [pool_id]} (path-params request)
         {:keys [with_items type
                 retired borrowable incomplete broken
                 inventory_pool_id owned in_stock
                 category_id
                 search before_last_check]} (query-params request)
         {:keys [page size]} (fetch-pagination-params request)
         query (-> (base-inventory-query pool_id)
                   (cond-> type (filter-by-type type))
                   (cond->
                    (and pool_id (true? with_items))
                     (with-items pool_id
                       :retired retired
                       :borrowable borrowable
                       :incomplete incomplete
                       :broken broken
                       :inventory_pool_id inventory_pool_id
                       :owned owned
                       :in_stock in_stock
                       :before_last_check before_last_check)

                     (and pool_id (false? with_items))
                     (without-items pool_id)

                     (and pool_id (presence search))
                     (with-search search))
                   (cond-> category_id
                     (#(from-category tx % category_id))))]
     (debug (sql-format query :inline true))

     (if (url-ends-with-uuid? (:uri request))
       (let [res (jdbc/execute-one! tx (-> query sql-format))]
         (if res
           (response res)
           (status 404)))
       (response (create-pagination-response request query with-pagination?))))))

(defn get-models-of-pool-with-pagination-handler [request]
  (get-models-handler request true))

(defn get-models-of-pool-handler [request]
  (let [result (get-models-handler request)]
    result))

(defn delete-attachments [{:keys [tx] :as request}]
  (let [{:keys [attachments_id]} (path-params request)
        res (jdbc/execute-one! tx
                               (-> (sql/delete-from :attachments)
                                   (sql/where [:= :id attachments_id])
                                   sql-format))]
    (if (= (:next.jdbc/update-count res) 1)
      (response {:status "ok" :attachments_id attachments_id})
      (bad-request {:error "Failed to delete attachment"}))))