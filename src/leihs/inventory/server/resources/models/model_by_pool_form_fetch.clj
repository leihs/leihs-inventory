(ns leihs.inventory.server.resources.models.model-by-pool-form-fetch
  (:require
   [clojure.data.codec.base64 :as b64]
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.java.io :as io]
   [clojure.pprint :refer [pprint]]
   [clojure.set]
   [clojure.set :as set]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.models.queries :refer [accessories-query attachments-query
                                                            entitlements-query item-query
                                                            model-links-query properties-query]]

   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]

   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.core :refer [single-entity-get-request?]]

   [leihs.inventory.server.utils.helper :refer [convert-map-if-exist]]
   [leihs.inventory.server.utils.pagination :refer [fetch-pagination-params pagination-response create-pagination-response]]
   [next.jdbc :as jdbc]
   [pantomime.extract :as extract]

   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]])
  (:import [java.net URL JarURLConnection]
           (java.time LocalDateTime)
           [java.util UUID]
           [java.util.jar JarFile]))
;
;(defn prepare-model-data
;  [data]
;  (let [;created-ts (:created_at data)
;        created-ts (LocalDateTime/now)
;
;        key-map {:type :type
;                 :manufacturer :manufacturer
;                 :product :product
;                 :version :version
;                 :hand_over_note :importantNotes ;;ok
;                 :description :description
;                 :internal_description :internal_description
;                 :technical_detail :technicalDetails}
;        renamed-data (->> key-map
;                          (reduce (fn [acc [db-key original-key]]
;                                    (if-let [val (get data original-key)]
;                                      (assoc acc db-key val)
;                                      acc))
;                                  {}))]
;    ;; Add default values or timestamps if needed
;    (assoc renamed-data
;           :type "Model"
;           :created_at created-ts
;           :updated_at created-ts)))
;
;(defn create-or-use-existing
;  [tx table where-values insert-values]
;  (let [select-query (-> (sql/select :*)
;                         (sql/from table)
;                         (sql/where where-values)
;                         sql-format)
;        existing-entry (first (jdbc/execute! tx select-query))]
;    (if existing-entry
;      existing-entry
;      (let [insert-query (-> (sql/insert-into table)
;                             (sql/values [insert-values])
;                             (sql/returning :*)
;                             sql-format)
;            new-entry (first (jdbc/execute! tx insert-query))]
;        new-entry))))
;
;(defn pr [str fnc]
;  ;(println ">oo> HELPER / " str fnc)(println ">oo> HELPER / " str fnc)
;  (println ">oo> " str fnc)
;  fnc)
;
;(defn parse-uuid-values
;  [key request]
;  (let [raw-value (get-in request [:parameters :multipart key])]
;    (cond
;      (instance? UUID raw-value) [raw-value]
;      (and (instance? String raw-value) (not (str/includes? raw-value ","))) [(UUID/fromString raw-value)]
;      (and (instance? String raw-value) (str/includes? raw-value ","))
;      (mapv #(UUID/fromString %) (str/split raw-value #",\s*"))
;      :else [])))
;
;(defn base-filename
;  "Removes `_thumb` suffix from filename to get the base filename for pairing, preserving the file extension."
;  [filename]
;  (if-let [[_ base extension] (re-matches #"(.*)_thumb(\.[^.]+)$" filename)]
;    (str base extension)
;    filename))
;
;(defn parse-json-array
;  [request key]
;  (let [json-array-string (get-in request [:parameters :multipart key])]
;    (if (and json-array-string (string? json-array-string))
;      (json/read-str (str "[" json-array-string "]") :key-fn keyword)
;      []))) ;; Return an empty vector if the value is nil or not a string
;
;(defn normalize-files
;  [request key]
;  (let [attachments (get-in request [:parameters :multipart key])]
;    (if (map? attachments)
;      [attachments]
;      attachments)))
;
;(defn base64-to-bytes [encoded-content]
;  (b64/decode (.getBytes encoded-content)))
;
;(defn file-to-base64 [file]
;  (when file
;    (let [bytes (with-open [in (io/input-stream file)
;                            out (java.io.ByteArrayOutputStream.)]
;                  (io/copy in out)
;                  (.toByteArray out))]
;      (String. (b64/encode bytes)))))
;
;(defn extract-metadata [file-path]
;  (let [metadata (extract/parse file-path)]
;    (pprint metadata)))


(defn select-entries
  [tx table columns where-clause]
  (jdbc/execute! tx
    (-> (apply sql/select columns)
      (sql/from table)
      (sql/where where-clause)
      sql-format)))



(defn base-pool-query [query pool-id]
  (-> query
    (sql/from [:models :m])
    (cond->
      pool-id (sql/join [:model_links :ml] [:= :m.id :ml.model_id])
      pool-id (sql/join [:model_groups :mg] [:= :mg.id :ml.model_group_id])
      pool-id (sql/join [:inventory_pools_model_groups :ipmg] [:= :mg.id :ipmg.model_group_id])
      pool-id (sql/join [:inventory_pools :ip] [:= :ip.id :ipmg.inventory_pool_id])
      pool-id (sql/where [:= :ip.id [:cast pool-id :uuid]]))))

(defn create-model-handler-by-pool-form-fetch [request]
  (let [
        created_ts (LocalDateTime/now)

        tx (get-in request [:tx])

        ;model-id (get-in request [:path-params :model_id])
        ;pool-id (to-uuid (get-in request [:path-params :pool_id]))

        model-id (to-uuid (get-in request [:path-params :model_id]))
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        p (println ">o> pool-id" pool-id (type pool-id))
        p (println ">o> model-id" model-id (type model-id))


        ]

    (try
      (let [

            res (jdbc/execute-one! tx (->
            ;(sql/select :m.*)
            (sql/select :m.id :m.product :m.manufacturer :m.version :m.type :m.hand_over_note :m.description
              :m.internal_description :m.technical_detail)
            ((fn [query] (base-pool-query query pool-id)))
                                        ;(sql/select :*)
                                        ;(sql/from :models)
                                          ;(sql/values [prepared-model-data])
                                          ;(sql/returning :*)
                                        (sql/where [:= :m.id model-id])
                                          sql-format))

            p (println ">o> res" res)

            res2 (select-entries tx :attachments [:id :filename :content_type] [:= :model_id model-id])
            p (println ">o> res2" res2)

            res3 (select-entries tx :accessories [:*] [:= :model_id model-id])
            p (println ">o> res3" res3)

            res4 (select-entries tx :models_compatibles [:*] [:= :model_id model-id])
            p (println ">o> res3" res4)


            res5 (select-entries tx :properties [:*] [:= :model_id model-id])
            p (println ">o> res3" res4)


            res6 (select-entries tx :entitlements [:*] [:= :model_id model-id])
            p (println ">o> res3" res6)


            res7 (-> (sql/select :e.* :eg.name :eg.inventory_pool_id)
                (sql/from [:entitlements :e])
                (sql/join [:entitlement_groups :eg] [:= :e.entitlement_group_id :eg.id])

                (sql/where [:= :e.id model-id])
                (sql/where [:= :eg.inventory_pool_id pool-id])
                ;(sql/returning :*)
                sql-format)
            res7 (jdbc/execute! tx res7)




            res (assoc res :attachments res2 :accessories res3 :compatibles res4 :properties res5
                      :entitlements res6 :entitlement_groups res7)

            ]


        (if res
          (response [res])
          (bad-request {:error "Failed to create model"})))
      (catch Exception e
        (error "Failed to fetch model" (.getMessage e))
        ;(error "Failed to create model" (.getMessage e))

        ;(cond
        ;  (str/includes? (.getMessage e) "unique_model_name_idx")
        ;  (-> (response {:status "failure"
        ;                 :message "Model already exists"
        ;                 :detail {:product (:product prepared-model-data)}})
        ;      (status 409))
        ;
        ;  (str/includes? (.getMessage e) "insert or update on table \"models_compatibles\"")
        ;  (-> (response {:status "failure"
        ;                 :message "Modification of models_compatibles failed"
        ;                 :detail {:product (:product prepared-model-data)}})
        ;      (status 409))
        ;
        ;  :else (bad-request {:error "Failed to create model" :details (.getMessage e)}))

        (bad-request {:error "Failed to fetch model" :details (.getMessage e)})

        ))))

