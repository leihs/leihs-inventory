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



(defn create-model-handler-by-pool-form [request]
  (let [
        created_ts (LocalDateTime/now)
        model-id (get-in request [:path-params :model_id])



        pool-id (get-in request [:path-params :pool_id])
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




        categories []


        ]
    (try
      (let [

            res (jdbc/execute-one! tx (-> (sql/insert-into :models)
                                          (sql/values [prepared-model-data])
                                          (sql/returning :*)
                                          sql-format))
            model-id (:id res)
            ;

            res true
            ]


        ;; Example usage:
        ;(doseq [category-id compatibles]
        ;  ;; Insert into model_links if not exists
        ;  (create-or-use-existing tx
        ;    :models_compatibles
        ;    [:and
        ;     [:= :model_id model-id]
        ;     [:= :compatible_id  category-id]]
        ;    {:model_id model-id :compatible_id  category-id}))
        ;
        ;;; Example usage:
        ;(doseq [category-id categories]
        ;  ;; Insert into model_links if not exists
        ;  (create-or-use-existing tx
        ;    :model_links
        ;    [:and
        ;     [:= :model_id model-id]
        ;     [:= :model_group_id (to-uuid category-id)]]
        ;    {:model_id model-id :model_group_id (to-uuid category-id)})
        ;
        ;  ;; Insert into inventory_pools_model_groups if not exists
        ;  (create-or-use-existing tx
        ;    :inventory_pools_model_groups
        ;    [:and
        ;     [:= :inventory_pool_id (to-uuid pool-id)]
        ;     [:= :model_group_id (to-uuid category-id)]]
        ;    {:inventory_pool_id (to-uuid pool-id) :model_group_id (to-uuid category-id)}))


        (if res
          (response [res])
          (bad-request {:error "Failed to create model"})))
      (catch Exception e
        (error "Failed to create model" (.getMessage e))
        (error "Failed to create model" (.getMessage e))
        (bad-request {:error "Failed to create model" :details (.getMessage e)})))))


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
