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
   [leihs.inventory.server.utils.pagination :refer [create-paginated-response pagination-response create-pagination-response]]
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

(defn sanitize-filename [filename]
  (str/replace filename #"[^a-zA-Z0-9_.-]" "_"))

(defn filter-keys [m keys-to-keep]
  (select-keys m keys-to-keep))

(defn filter-keys-attachments [m]
  (filter-keys m [:filename :content_type :size :model_id :item_id :content]))

(defn attachment-response-format [s]
  (sql/returning s :id :filename))

(defn post-resource [req]
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




(defn index-resources [request]
  (try
    (let [tx (:tx request)
          model-id (-> request path-params :model_id)
          accept-header (get-in request [:headers "accept"])
          content-disposition (or (-> request :parameters :query :content_disposition) "inline")
          type (or (-> request :parameters :query :type) "new")
          query (-> (sql/select :a.*)
                  (sql/from [:attachments :a])
                  (cond-> model-id (sql/where [:= :a.model_id model-id]))                  )
          ]

      (let [{:keys [page size]} (fetch-pagination-params request)

            p (println ">o> abc.???" page size)
            ]
        (response (create-paginated-response query tx size page)))



      )

(catch Exception e
      (error "Failed to get attachments" e)
      (bad-request {:error "Failed to get attachments" :details (.getMessage e)}))))


