(ns leihs.inventory.server.resources.models.model-by-pool-form
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.models.queries :refer [accessories-query attachments-query base-pool-query
                                                            entitlements-query item-query
                                                            model-links-query properties-query]]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.core :refer [single-entity-get-request?]]
   [leihs.inventory.server.utils.helper :refer [convert-map-if-exist]]
   [leihs.inventory.server.utils.pagination :refer [fetch-pagination-params pagination-response create-pagination-response]]
   [next.jdbc :as jdbc]
   [clojure.string :as str]

   [clojure.java.io :as io]

   [clojure.set :as set]

   [clojure.data.json :as json]

   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]])
  (:import [java.net URL JarURLConnection]
           (java.time LocalDateTime)
           [java.util.jar JarFile]
   [java.util UUID]
   ))


(defn prepare-model-data
  [data]
  (let [
        ;created-ts (:created_at data)
        created-ts (LocalDateTime/now)

        key-map {:type                 :type
                 :manufacturer         :manufacturer
                 :product              :product
                 :version              :version
                 :hand_over_note       :importantNotes      ;;ok
                 :description          :description
                 :internal_description :internal_description
                 :technical_detail     :technicalDetails}
        renamed-data (->> key-map
                       (reduce (fn [acc [db-key original-key]]
                                 (if-let [val (get data original-key)]
                                   (assoc acc db-key val)
                                   acc))
                         {}))]
    ;; Add default values or timestamps if needed
    (assoc renamed-data
      :type "Model"
      :created_at created-ts
      :updated_at created-ts)))


(defn create-or-use-existing
  [tx table where-values insert-values]
  (let [query (-> (sql/select 1)
                (sql/from table)
                (sql/where where-values)
                sql-format)]
    (when (empty? (jdbc/execute! tx query))
      (jdbc/execute! tx (-> (sql/insert-into table)
                          (sql/values [insert-values])
                          sql-format)))))

(defn create-or-use-existing
  [tx table where-values insert-values]
  (let [select-query (-> (sql/select :*)
                       (sql/from table)
                       (sql/where where-values)
                       sql-format)
        existing-entry (first (jdbc/execute! tx select-query))]
    (if existing-entry
      existing-entry
      (let [insert-query (-> (sql/insert-into table)
                           (sql/values [insert-values])
                           (sql/returning :*)
                           sql-format)
            new-entry (first (jdbc/execute! tx insert-query))]
        new-entry))))

;(defn parse-uuid-values
;[key request]
;(let [raw-value (get-in request [:parameters :multipart key])]
;  (if (and raw-value (not (clojure.string/blank? raw-value)))
;    (mapv uuid (str/split raw-value #",\s*"))
;    [])))


(defn pr [str fnc]
  ;(println ">oo> HELPER / " str fnc)(println ">oo> HELPER / " str fnc)
  (println ">oo> " str fnc)
  fnc
  )


(defn parse-uuid-values
  [key request]
  (let [raw-value (get-in request [:parameters :multipart key])]
    (cond
      (instance? UUID raw-value) [raw-value]
      (and (instance? String raw-value) (not (str/includes? raw-value ","))) [(UUID/fromString raw-value)]
      (and (instance? String raw-value) (str/includes? raw-value ","))
      (mapv #(UUID/fromString %) (str/split raw-value #",\s*"))
      :else [])))



;(require '[clojure.data.json :as json])

(defn parse-json-array
  [request key]
  (let [json-array-string (get-in request [:parameters :multipart key])]
    (if (and json-array-string (string? json-array-string))
      (json/read-str (str "[" json-array-string "]") :key-fn keyword)
      []))) ;; Return an empty vector if the value is nil or not a string


(defn rename-content-type
  [file-map]
  (set/rename-keys file-map {:content-type :content_type}))


(defn normalize-files
  [request key]
  (let [attachments (get-in request [:parameters :multipart key])]
    (if (map? attachments)
      [attachments]
      attachments)))


(defn create-model-handler-by-pool-form [request]
  (let [
        created_ts (LocalDateTime/now)
        model-id (get-in request [:path-params :model_id])



        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        p (println ">o> pool-id" pool-id)

        multipart (get-in request [:parameters :multipart])
        p (println ">o> multipart" multipart)               ;; this

        body-params (:body-params request)
        p (println ">o> body-params" body-params)
        tx (:tx request)

        p (println ">o> create-model-handler-by-pool.body-params" body-params)

        model (assoc body-params :created_at created_ts :updated_at created_ts)
        ;categories (:category_ids model)
        ;model (dissoc model :category_ids)


        ;created_ts (LocalDateTime/now)
        ;data {:type "Model"  :created_at created_ts :updated_at created_ts}

        ;; --------------


        prepared-model-data (prepare-model-data multipart)
        p (println ">o> rename-keys-to-db-names" prepared-model-data)


        ;key (key "compatibles")
        ;multipart (get-in request [:parameters :multipart key])

        compatibles (parse-uuid-values :compatible_ids request)
        p (println ">o> ??? compatibles" compatibles)

        categories (parse-uuid-values :category_ids request)
        p (println ">o> ??? categories" categories)


        ;attachments (get-in request [:parameters :multipart :attachments])
        ;attachments (if (= (type attachments) clojure.lang.PersistentArrayMap)
        ;               (vector attachments)
        ;               attachments
        ;               )

        attachments (normalize-files request :attachments)
        images (normalize-files request :images)


        p (println ">o> attachments ???1" attachments (type attachments))






        ;accessories (get-in request [:parameters :multipart :accessories])
        ;p (println ">o> ??? accessories" accessories (type accessories))
        ;>o> accessories ???2b [{:key string, :value string}] clojure.lang.PersistentVector
        properties (parse-json-array request :properties)
        p (println ">o> properties ???1" properties (type properties))

        ; [{:name string1, :inventory_bool true} {:name string2, :inventory_bool false}]
        accessories (parse-json-array request :accessories)
        p (println ">o> accessories ???1" accessories (type accessories))
        ;p (println ">o> accessories ???2a" accessories )
        ;p (println ">o> accessories ???2b" accessories (type accessories))


        entitlements (parse-json-array request :entitlements)
        p (println ">o> entitlements ???1" entitlements (type entitlements))


        ;categories []


        ]
    (try
      (let [

            res (jdbc/execute-one! tx (-> (sql/insert-into :models)
                                          (sql/values [prepared-model-data])
                                          (sql/returning :*)
                                          sql-format))
            model-id (:id res)
            ;

            res model-id
            ]


        ; Example usage: attachments
        (doseq [entry attachments]
            (let [;; Rename `:content-type` to `:content_type`

                  p (println ">o> abc.before" entry)
                  data (set/rename-keys entry {:content-type :content_type})
                  p (println ">o> abc.after" data)

                  ;; Extract the file reference
                  file (:tempfile data)


                  ;; Fetch the content from the file
                  file-content (when file (slurp (io/input-stream file)))

                  ;; Remove `:tempfile` from `data` to store metadata only
                  data (dissoc data :tempfile)

                  p (println ">o> abc.2before" data)
                  data (assoc data :content file-content :model_id model-id)
                  p (println ">o> abc.2after" data)


                  p (println ">o> !!!!!!!! data" (keys data))

                  ;; Insert metadata into the `attachments` table
                  res (-> (sql/insert-into :attachments)
                        (sql/values [data])
                        (sql/returning :*)
                        sql-format)

                  res (jdbc/execute! tx res)

                  ;; Debugging output
                  _ (println ">o> >>> !!!!!!! DONE !!!!!!!! attachments.res" res)
                  ;_ (println ">o> >>> file metadata" data)
                  ;_ (println ">o> >>> file content (first 100 chars):" (subs file-content 0 (min 100 (count file-content))))

                  ]

              ;; Process `res` and `file-content` as needed
              ))

        ; Example usage: images
        ; - validate image if thumbnail is present
        ; - generate uuid for target_id

        ; 1. insert image, thumbnail==false
        ; 2. insert thumbnail and set parent_id==image.id, thumbnail==false
        (doseq [entry images]
            (let [;; Rename `:content-type` to `:content_type`

                  p (println ">o> abc.before" entry)
                  data (set/rename-keys entry {:content-type :content_type})
                  p (println ">o> abc.after" data)

                  ;; Extract the file reference
                  file (:tempfile data)


                  ;; Fetch the content from the file
                  file-content (when file (slurp (io/input-stream file)))

                  ;; Remove `:tempfile` from `data` to store metadata only
                  data (dissoc data :tempfile)

                  p (println ">o> abc.2before" data)
                  ;data (assoc data :content file-content :model_id model-id :target_type "Model")
                  data (assoc data :content file-content :target_type "Model")
                  p (println ">o> abc.2after" data)


                  p (println ">o> !!!!!!!! data" (keys data))

                  ;; Insert metadata into the `attachments` table
                  res (-> (sql/insert-into :images)
                        (sql/values [data])
                        (sql/returning :*)
                        sql-format)

                  res (jdbc/execute! tx res)

                  ;; Debugging output
                  _ (println ">o> >>> !!!!!!! DONE !!!!!!!! attachments.res" res)
                  ;_ (println ">o> >>> file metadata" data)
                  ;_ (println ">o> >>> file content (first 100 chars):" (subs file-content 0 (min 100 (count file-content))))

                  ]

              ;; Process `res` and `file-content` as needed
              ))

        ; Example usage: entitlements
        (doseq [entry entitlements]

          (let [
                ;; Insert into model_links if not exists
                res (create-or-use-existing tx
                      :entitlements
                      [:and
                       [:= :model_id model-id]
                       [:= :entitlement_group_id  (to-uuid (:entitlement_group_id entry))]]
                      {:model_id model-id :entitlement_group_id  (to-uuid (:entitlement_group_id entry)) :quantity  (:quantity entry)})
                p (println ">o> >>> entitlements.res" res)

                ])
          )


        ; Example usage: properties
        (doseq [entry properties]

             (let [
          ;; Insert into model_links if not exists
          res (create-or-use-existing tx
            :properties
            [:and
             [:= :model_id model-id]
             [:= :key  (:key entry)]]
            {:model_id model-id :key  (:key entry) :value  (:value entry)})
          p (println ">o> >>> properties.res" res)

                      ])
          )


        ; Example usage: accessories
        (doseq [entry accessories]
          (let [
                 _  (println ">o> accessories.e" entry)

                 accessory (create-or-use-existing tx
                    :accessories
                    [:and
                     [:= :model_id model-id]
                     [:= :name  (:name entry)]]
                    {:model_id model-id :name  (:name entry)})

                p (println ">o> >>> accessory1" accessory)

                accessory-id (:id accessory)
                p (println ">o> accessory1.accessory-id" accessory-id)
                p (println ">o> accessory1.pool-id" pool-id)
                p (println ">o> accessory1.inventory_bool" (:inventory_bool entry))

                inv-pool-entry (if (:inventory_bool entry)
                    (create-or-use-existing tx
                      :accessories_inventory_pools
                      [:and
                       [:= :accessory_id accessory-id]
                       [:= :inventory_pool_id  pool-id]]
                      {:accessory_id accessory-id :inventory_pool_id pool-id})
                    nil
                    )

                p (println ">o> >>> inv-pool-entry2" inv-pool-entry)
                ])
         )



        ; Example usage: compatibles
        (doseq [category-id compatibles]
          ;; Insert into model_links if not exists
          (create-or-use-existing tx
            :models_compatibles
            [:and
             [:= :model_id model-id]
             [:= :compatible_id  category-id]]
            {:model_id model-id :compatible_id  category-id}))

        ;; Example usage: categories
        (doseq [category-id categories]
          ;; Insert into model_links if not exists
          (create-or-use-existing tx
            :model_links
            [:and
             [:= :model_id model-id]
             [:= :model_group_id (to-uuid category-id)]]
            {:model_id model-id :model_group_id (to-uuid category-id)})

          ;; Insert into inventory_pools_model_groups if not exists
          (create-or-use-existing tx
            :inventory_pools_model_groups
            [:and
             [:= :inventory_pool_id (to-uuid pool-id)]
             [:= :model_group_id (to-uuid category-id)]]
            {:inventory_pool_id (to-uuid pool-id) :model_group_id (to-uuid category-id)}))


        (if res
          (response [res])
          (bad-request {:error "Failed to create model"})))
      (catch Exception e
        (error "Failed to create model" (.getMessage e))
        (error "Failed to create model" (.getMessage e))

        (cond
          (str/includes? (.getMessage e) "unique_model_name_idx")
          (-> (response {:status "failure"
                                  :message "Model already exists"
                                  :detail {:product (:product prepared-model-data)}                         })
            (status 409))

          (str/includes? (.getMessage e) "insert or update on table \"models_compatibles\"")
          (-> (response {:status "failure"
                                  :message "Modification of models_compatibles failed"
                                  :detail {:product (:product prepared-model-data)}                         })
            (status 409))

          :else         (bad-request {:error "Failed to create model" :details (.getMessage e)})          )
        ))))


;
;(defn update-model-handler-by-pool [request]
;  (let [model-id (get-in request [:path-params :model_id])
;        body-params (:body-params request)
;        tx (:tx request)]
;    (try
;      (let [res (jdbc/execute! tx (-> (sql/update :models)
;                                      (sql/set (convert-map-if-exist body-params))
;                                      (sql/where [:= :id (to-uuid model-id)])
;                                      (sql/returning :*)
;                                      sql-format))]
;        (if (= 1 (count res))
;          (response res)
;
;          (bad-request {:error "Failed to update model" :details "Model not found"})))
;      (catch Exception e
;        (error "Failed to update model" e)
;        (bad-request {:error "Failed to update model" :details (.getMessage e)})))))
;
;(defn delete-model-handler-by-pool [request]
;  (let [tx (:tx request)
;        model-id (get-in request [:path-params :model_id])]
;    (try
;      (let [res (jdbc/execute! tx (-> (sql/delete-from :models)
;                                      (sql/where [:= :id (to-uuid model-id)])
;                                      (sql/returning :*)
;                                      sql-format))]
;        (if (= 1 (count res))
;          (response res)
;          (bad-request {:error "Failed to delete model" :details "Model not found"})))
;      (catch Exception e
;        (error "Failed to delete model" e)
;        (status (bad-request {:error "Failed to delete model" :details (.getMessage e)}) 409)))))
