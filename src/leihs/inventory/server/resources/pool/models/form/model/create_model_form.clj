(ns leihs.inventory.server.resources.pool.models.form.model.create-model-form
  (:require
   [cheshire.core :as cjson]
   [clojure.data.codec.base64 :as b64]
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.models.form.model.common-model-form :refer :all]
   [leihs.inventory.server.resources.pool.models.form.model.common-model-form :refer [extract-model-form-data
                                                                      filter-response]]
   [leihs.inventory.server.resources.pool.models.helper :refer [base-filename
                                                           normalize-files normalize-model-data
                                                           parse-json-array process-attachments  file-sha256]]
   [leihs.inventory.server.resources.pool.common :refer [str-to-bool  remove-nil-entries create-image-url ]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.exception-handler :refer [exception-to-response]]
   [next.jdbc :as jdbc]
   [pantomime.extract :as extract]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]])
  (:import [java.net URL JarURLConnection]
           (java.time LocalDateTime)
           [java.util UUID]
           [java.util.jar JarFile]))

(defn create-model-handler-by-pool-form [request]
  (let [validation-result (atom [])
        created-ts (LocalDateTime/now)
        tx (:tx request)
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        {:keys [accessories prepared-model-data categories compatibles attachments properties
                entitlements images new-images-attr existing-images-attr]}
        (extract-model-form-data request)]

    (try
      (let [res (jdbc/execute-one! tx (-> (sql/insert-into :models)
                                          (sql/values [prepared-model-data])
                                          (sql/returning :*)
                                          sql-format))
            res (filter-response res [:rental_price])
            model-id (:id res)]

        (process-entitlements tx entitlements model-id)
        (process-properties tx properties model-id)
        (process-accessories tx accessories model-id pool-id)
        (process-compatibles tx compatibles model-id)
        (process-categories tx categories model-id pool-id)
        (if res
          (response (create-validation-response res @validation-result))
          (bad-request {:error "Failed to create model"})))
      (catch Exception e

        (exception-to-response request e "Failed to create model")))))
(defn create-model-handler-by-pool-model-json [request]
  (create-model-handler-by-pool-form request))
