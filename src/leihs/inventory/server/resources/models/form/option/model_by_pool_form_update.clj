(ns leihs.inventory.server.resources.models.form.option.model-by-pool-form-update
  (:require
   [cheshire.core :as cjson]
   [clojure.data.codec.base64 :as b64]
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.models.form.license.common :refer [double-to-numeric-or-nil]]
   [leihs.inventory.server.resources.models.helper :refer [str-to-bool normalize-model-data parse-json-array normalize-files
                                                           file-to-base64 base-filename process-attachments]]
   [leihs.inventory.server.resources.models.queries :refer [accessories-query attachments-query base-pool-query
                                                            entitlements-query item-query
                                                            model-links-query properties-query]]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.helper :refer [convert-map-if-exist]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]])
  (:import [java.net URL JarURLConnection]
           (java.time LocalDateTime)
           [java.util UUID]))

(defn update-option-handler-by-pool-form [request]
  (let [option-id (to-uuid (get-in request [:path-params :option_id]))
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        multipart (get-in request [:parameters :multipart])
        tx (:tx request)

        price (double-to-numeric-or-nil (:price multipart))
        multipart (assoc multipart :price price)]
    (try
      (let [update-model-query (-> (sql/update :options)
                                   (sql/set multipart)
                                   (sql/where [:= :id option-id])
                                   (sql/returning :*)
                                   sql-format)
            updated-model (jdbc/execute-one! tx update-model-query)]

        (if updated-model
          (response [updated-model])
          (bad-request {:error "Failed to update model"})))
      (catch Exception e
        (error "Failed to update model" (.getMessage e))
        (bad-request {:error "Failed to update model" :details (.getMessage e)})))))
