(ns leihs.inventory.server.resources.models.form.license.model-by-pool-form-update
  (:require
   [cheshire.core :as cjson]
   [clojure.data.codec.base64 :as b64]
   [clojure.data.json :as json]
   [cheshire.core :as jsonc]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.models.form.model.model-by-pool-form-fetch :refer [create-model-handler-by-pool-form-fetch]]
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



(defn process-deletions [tx ids table key]
  (println ">o> process-deletions" table key )
  (doseq [id ids]
    (println ">o> process-deletions.id" id)
    (jdbc/execute! tx (-> (sql/delete-from table)
                          (sql/where [:= key (to-uuid id)])
                          sql-format))))


(defn update-license-handler-by-pool-form [request]
  (let [
        now-ts (LocalDateTime/now)

        item-id (to-uuid (get-in request [:path-params :item_id]))
        model-id (to-uuid (get-in request [:path-params :model_id]))
        pool-id (to-uuid (get-in request [:path-params :pool_id]))

        multipart (get-in request [:parameters :multipart])
        tx (:tx request)

        p (println ">o> ??? model-id" model-id)
        p (println ">o> ??? item-id" item-id)


        multipart (get-in request [:parameters :multipart])
        p (println ">o> multipart1" multipart)


        properties (first (parse-json-array request :properties))
        p (println ">o> properties" properties)


        multipart2 (dissoc multipart :attachments :properties :retired :invoice_date :price :attachments-to-delete)
        multipart2b {
                     :updated_at now-ts

                     :properties [:cast (jsonc/generate-string properties) :jsonb]

                     :owner_id (to-uuid "8bd16d45-056d-5590-bc7f-12849f034351")
                     :inventory_pool_id pool-id

                     :room_id (to-uuid "503870e1-7fe5-44ef-89e7-11f1c40a9e70")
                     }

        multipart2 (merge multipart2 multipart2b)

        p (println ">o> ??? multipart2" multipart2)


        ]
    (try
      (let [update-model-query (-> (sql/update :items)
                                   ;(sql/set prepared-model-data)
                                   (sql/set multipart2)
                                   (sql/where [:= :id item-id])
                                   (sql/returning :*)
                                   sql-format)
            updated-model (jdbc/execute-one! tx update-model-query)


            p (println ">o> >> updated-model1" (:id updated-model))
            p (println ">o> >> updated-model2" updated-model)

            attachments (normalize-files request :attachments)
            attachments-to-delete (parse-json-array request :attachments-to-delete)

            p (println ">o> >>>>>>>>>>>>>>>>>> attachments-to-delete" attachments-to-delete)

            _ (do
              (process-attachments tx attachments "item_id" (:id updated-model))
                    (process-deletions tx attachments-to-delete :attachments :id))



            p (println ">o> !!!!!!!!!!! 2 abc.item_id" item-id)
            res (jdbc/execute! tx  (-> (sql/select :id :filename :content_type :size)
                                     (sql/from :attachments)
                                     (sql/where [:= :item_id item-id])
                                     sql-format)
                  )
            ;p (println ">o> ????abc1" res)
            updated-model (assoc updated-model :attachments res)

         ]
        (if updated-model
          (response [updated-model])
          (bad-request {:error "Failed to update model"})))
      (catch Exception e
        (error "Failed to update model" (.getMessage e))
        (bad-request {:error "Failed to update model" :details (.getMessage e)})))))
