(ns leihs.inventory.server.resources.models.model-by-pool-form-update
  (:require
   [clojure.data.codec.base64 :as b64]
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.pprint :refer [pprint]]
   [clojure.set :as set]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.models.queries :refer [accessories-query attachments-query base-pool-query
                                                            entitlements-query item-query
                                                            model-links-query properties-query]]

   [leihs.inventory.server.resources.models.model-by-pool-form-fetch :refer [ create-model-handler-by-pool-form-fetch]]

   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.helper :refer [convert-map-if-exist]]
   [next.jdbc :as jdbc]
   [pantomime.extract :as extract]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]])
  (:import [java.net URL JarURLConnection]
   (java.time LocalDateTime)
   [java.util UUID]))

(defn parse-uuid-values
  "Parses UUIDs from a comma-separated string or single string. Returns a vector of UUIDs."
  [key request]
  (let [raw-value (get-in request [:parameters :multipart key])]
    (cond
      (instance? UUID raw-value) [raw-value]
      (and (instance? String raw-value) (not (str/includes? raw-value ","))) [(UUID/fromString raw-value)]
      (and (instance? String raw-value) (str/includes? raw-value ","))
      (mapv #(UUID/fromString %) (str/split raw-value #",\s*"))
      :else [])))

(defn prepare-model-data
  [data]
  (let [key-map {:type :type
                 :manufacturer :manufacturer
                 :product :product
                 :version :version
                 :hand_over_note :importantNotes
                 :description :description
                 :internal_description :internalDescription
                 :technical_detail :technicalDetails}
        created-ts (LocalDateTime/now)
        renamed-data (reduce (fn [acc [db-key original-key]]
                               (if-let [val (get data original-key)]
                                 (assoc acc db-key val)
                                 acc))
                       {} key-map)]
    (assoc renamed-data :updated_at created-ts)))


(defn pr [str fnc]
  ;(println ">oo> HELPER / " str fnc)(println ">oo> HELPER / " str fnc)
  ;(println ">oo> " str fnc)
  (println ">oo> " str)
  fnc
  )

(defn update-or-insert
  [tx table where-values update-values]
  (let [select-query (-> (sql/select :*)
                       (sql/from table)
                       (sql/where where-values)
                       sql-format)
        existing-entry (first (jdbc/execute! tx select-query))]
    (if existing-entry
      (let [update-query (-> (sql/update table)
                           (sql/set update-values)
                           (sql/where where-values)
                           (sql/returning :*)
                           sql-format)]
        (pr "update" (jdbc/execute-one! tx update-query)))
      (let [insert-query (-> (sql/insert-into table)
                           (sql/values [update-values])
                           (sql/returning :*)
                           sql-format)]
        (pr "insert" (jdbc/execute-one! tx insert-query))))))


(defn insert-if-not-exists
  [tx table where-values insert-values]
  (let [select-query (-> (sql/select :*)
                       (sql/from table)
                       (sql/where where-values)
                       sql-format)
        existing-entry (first (jdbc/execute! tx select-query))]
    (if (nil? existing-entry)
      (let [insert-query (-> (sql/insert-into table)
                           (sql/values [insert-values])
                           (sql/returning :*)
                           sql-format)]
        (pr "insert" (jdbc/execute-one! tx insert-query)))
      (println "Entry already exists, skipping insert"))))



(defn update-insert-or-delete
  [tx table where-values update-values entry]

  (println ">o> ??? update-insert-or-delete" update-values)

  (if (:delete entry)
    ;; Perform delete operation if :delete key is present and true
    (let [delete-query (-> (sql/delete-from table)
                         (sql/where where-values)
                         sql-format)]
      (pr "delete" (jdbc/execute-one! tx delete-query)))

    ;; Otherwise, perform update-or-insert as usual
    (let [select-query (-> (sql/select :*)
                         (sql/from table)
                         (sql/where where-values)
                         sql-format)
          existing-entry (first (jdbc/execute! tx select-query))]
      (if existing-entry
        ;; Update if entry exists
        (let [update-query (-> (sql/update table)
                             (sql/set update-values)
                             (sql/where where-values)
                             (sql/returning :*)
                             sql-format)]
          (pr "update" (jdbc/execute-one! tx update-query)))
        ;; Insert if entry does not exist
        (let [insert-query (-> (sql/insert-into table)
                             (sql/values [update-values])
                             (sql/returning :*)
                             sql-format)]
          (pr "insert" (jdbc/execute-one! tx insert-query)))))))



(defn normalize-files
  [request key]
  (let [attachments (get-in request [:parameters :multipart key])
        normalized (if (map? attachments)
                     [attachments]
                     ;(vector attachments)
                     attachments)

        _ (println ">o> before normalize-files" normalized (type normalized))
        ;; Filter out entries with :size 0
        filtered  (vec (filter #(pos? (:size % 0)) normalized))

        ]

    ;; Print the filtered files for debugging
    (println ">o> normalize-files" filtered (type filtered))

    ;; Return the filtered files
    filtered))


(defn select-entries
  [tx table columns where-clause]
  (jdbc/execute! tx
    (-> (apply sql/select columns)
      (sql/from table)
      (sql/where where-clause)
      sql-format)))


(defn pr2 [str fnc]
  ;(println ">oo> HELPER / " str fnc)(println ">oo> HELPER / " str fnc)
  (println ">oo> " str )
  fnc
  )

(defn parse-json-array
  [request key]
  (let [json-array-string (get-in request [:parameters :multipart key])

        p (println ">o> parse-json-array0" key)
        p (println ">o> parse-json-array1" (type json-array-string))

        p (println ">o> parse-json-array2" json-array-string (type json-array-string) (string? json-array-string))
        ]
    (cond
      (not json-array-string) (pr2 "1"[])
      (and (string? json-array-string) (some #(= json-array-string %) ["" "[]"])) (pr2 "2"[])
      ;:else (json/read-str (str "[" json-array-string "]") :key-fn keyword))
      :else (pr2 "3" (json/read-str  json-array-string :key-fn keyword))
      )
    ))


(defn update-model-handler-by-pool-form [request]
  (let [
        p (println ">o> update-model-handler-by-pool-form !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")

        model-id (to-uuid(get-in request [:path-params :model_id]))
        pool-id (to-uuid (get-in request [:path-params :pool_id]))

        p (println ">o> abc" model-id (type model-id) pool-id (type pool-id))
        p (println ">o> abc1")

        multipart (get-in request [:parameters :multipart])
        p (println ">o> abc2")
        tx (:tx request)
        p (println ">o> abc3")
        prepared-model-data (prepare-model-data multipart)
        p (println ">o> abc4")


        ]
    (try
      (let [update-model-query (-> (sql/update :models)
                                 (sql/set prepared-model-data)
                                 (sql/where [:= :id model-id])
                                 (sql/returning :*)
                                 sql-format)
            updated-model (jdbc/execute-one! tx update-model-query)

        p (println ">o> abc4")
            ]

        ;; Update associated tables with parsed values
        (let [

              ;compatibles (parse-uuid-values :compatible_ids request)
              compatibles (parse-json-array request :compatibles )

              ;categories (parse-uuid-values :category_ids request)
              categories (parse-json-array request :categories )
              p (println ">o> abc.categories" categories)   ;;fails
              ;p (println ">o> abc.categories2" (:categories multipart) (type (:categories multipart))) ;;ok
              ;p (println ">o> abc.categories3" (keys multipart))
              ;categories (:categories multipart)


              attachments (normalize-files request :attachments)
              images (normalize-files request :images)
              properties (parse-json-array request :properties)


              p (println ">o> properties ??" properties)

              accessories (parse-json-array request :accessories)
              entitlements (parse-json-array request :entitlements)]


           (println ">o> abX.1" )

          ;; Update entitlements
          (doseq [entry entitlements]

            (let [
                  id (to-uuid(:entitlement_id entry))
                  where-clause (if (nil? id)
                                 [:and [:= :model_id model-id] [:= :entitlement_group_id (to-uuid (:entitlement_group_id entry))]]
                                 [:and [:= :id id] [:= :model_id model-id]]
                                 )


                  p (println ">o> entitlements ?? " id (:delete entry))
                  ]

            (update-insert-or-delete tx
              :entitlements
              ;[:and [:= :model_id model-id] [:= :entitlement_group_id (to-uuid (:entitlement_group_id entry))]]
              where-clause
              {:model_id model-id :entitlement_group_id (to-uuid (:entitlement_group_id entry)) :quantity (:quantity entry)} entry)))

           (println ">o> abX.2" )



          ;; Update properties
          (doseq [entry properties]

            p (println ">o> ??? entry" entry)
          (let [
                id (to-uuid(:id entry))
                where-clause (if (nil? id)
                               [:and [:= :model_id model-id] [:= :key (:key entry)]]
                               [:and [:= :id id] [:= :model_id model-id]]
                        )
                ]


            (update-insert-or-delete tx
              :properties
              where-clause
              {:model_id model-id :key (:key entry) :value (:value entry)} entry))
            )





           (println ">o> abX.3" )
          ;; Update accessories
          (doseq [entry accessories]

                    (println ">o> DELETE!!!" entry)
            (if (:delete entry)
              (let [                                        ;; DELETE

                    p (println ">o> DELETE!!!" (:delete entry))

                    id (to-uuid(:id entry))
                    query (-> (sql/delete-from :accessories_inventory_pools)
                               (sql/where [:= :accessory_id id] [:= :inventory_pool_id pool-id])
                               sql-format)
                    res (try (jdbc/execute! tx query) (catch Exception e (error "Failed to delete :accessories_inventory_pools" (.getMessage e))))
                    p (println ">o> del1.res" res)

                    query (-> (sql/delete-from :accessories)
                               (sql/where [:= :id id])
                               sql-format)
                    ;res (jdbc/execute! tx query)
                    res (try (jdbc/execute! tx query) (catch Exception e (error "Failed to delete :accessories" (.getMessage e))))
                    p (println ">o> del2.res" res)                                                  ])



            (let [                                          ;; UPDATE/INSERT
                  accessory-id (to-uuid(:id entry))

                  where-clause (if (nil? accessory-id)
                                 [:and [:= :model_id model-id] [:= :name (:name entry)]]
                                 [:= :id accessory-id]
                                 )

                  ;accessory (jdbc/execute! tx (-> (sql/select :*)
                  ;                       (sql/from :accessories)
                  ;                       ;(sql/where [:and [:= :model_id model-id] [:= :name (:name entry)]])
                  ;                       (sql/where  [:= :id accessory-id] )
                  ;                       sql-format)

                  accessory (update-or-insert tx
                              :accessories
                              ;[:and [:= :model_id model-id] [:= :name (:name entry)]]
                              where-clause
                              {:model_id model-id :name (:name entry)})
                  accessory-id (:id accessory)
                  ]
              (when (:inventory_bool entry)
                (update-or-insert tx
                  :accessories_inventory_pools
                  [:and [:= :accessory_id accessory-id] [:= :inventory_pool_id pool-id]]
                  {:accessory_id accessory-id :inventory_pool_id pool-id})))


            )
            )






           (println ">o> abX.4" )
          ;; Update compatible models
          (doseq [compatible compatibles]


            (let [
                  ;compatible-id (to-uuid (:model_id compatible))
                  compatible-id (to-uuid (:id compatible))
                  p (println ">o> ???? compatible-id" compatible-id model-id)
                  p (println ">o> ???? compatible-id.compatible" compatible)
                  ;where-clause (if (nil? compatible-id)
                  ;               [:and [:= :model_id model-id] [:= :compatible_id compatible-id]]
                  ;               [:and [:= :id compatible-id] [:= :model_id model-id]]
                  ;               )

                  where-clause [:and [:= :model_id model-id] [:= :compatible_id compatible-id]]
                  ]


            (update-insert-or-delete tx
              :models_compatibles
              ;[:and [:= :model_id model-id] [:= :compatible_id compatible-id]]
              where-clause
              {:model_id model-id :compatible_id compatible-id}
              compatible
              ))
              )



           (println ">o> abX.5" )
          ;; Update model categories, insert only??
          (doseq [category categories]

             (let [
                      p (println ">o> category ????" category)
                      p (println ">o> category ???? del?" (:delete category))

                   category-id (to-uuid (:id category))

                      ]

               (if (:delete category)
                 (let [
                   res (jdbc/execute! tx (-> (sql/delete-from :model_links)
                                         (sql/where [:= :model_id model-id] [:= :model_group_id category-id])
                                         sql-format))

                       p (println ">o> delete.category.res" res)

                       ]
                   (println ">o> DELETE CATEGORY" ))
                 (do
            (update-or-insert tx
              :model_links
              [:and [:= :model_id model-id] [:= :model_group_id category-id]]
              {:model_id model-id :model_group_id category-id})
            (update-or-insert tx
              :inventory_pools_model_groups
              [:and [:= :inventory_pool_id pool-id] [:= :model_group_id category-id]]
              {:inventory_pool_id pool-id :model_group_id category-id}))


                   )

                 )
               )




           (println ">o> abX.6" )
          ;; Additional file handling for attachments and images (similar to existing logic)

          (if updated-model
            (response [updated-model])

            ;(create-model-handler-by-pool-form-fetch request)

            (bad-request {:error "Failed to update model"}))))
      (catch Exception e
        (error "Failed to update model" (.getMessage e))
        (bad-request {:error "Failed to update model" :details (.getMessage e)})))))
