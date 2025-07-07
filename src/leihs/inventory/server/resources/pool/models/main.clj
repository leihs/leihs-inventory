(ns leihs.inventory.server.resources.pool.models.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.core :refer [presence]]
   [leihs.inventory.server.resources.pool.models.queries :refer [base-inventory-query
                                                                 filter-by-type
                                                                 from-category with-items with-search
                                                                 without-items]]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.helper :refer [url-ends-with-uuid?]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response fetch-pagination-params]]
   [next.jdbc :as jdbc]
   ;[honeysql.core :as h]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [debug error]])

(:import [java.net URL JarURLConnection]
 (java.time LocalDateTime)
 [java.util.jar JarFile]))



(defn get-first-thumbnail-query [ids]
  (let [ids (or ids [])

        p (println ">o> abc.type?" (type ids))
        ]
    (when (seq ids)
      {:select [:id :target_id :thumbnail :filename]
       :from [[{:select [:id :target_id :thumbnail :filename
                         [[:row-number
                           [:over {:partition-by [:target_id]
                                   :order-by [[:id :asc]]}]] :rn]]
                :from [:images]
                :where [:and
                        [:in :target_id ids]
                        [:= :thumbnail true]]}
               :t]]
       :where [:= :rn 1]})))



(defn get-first-thumbnail-query [ids]
  (-> (sql/select :id :target_id :thumbnail :filename)
    (sql/from [[:raw
                (str "(SELECT id, target_id, thumbnail, filename, "
                  "ROW_NUMBER() OVER (PARTITION BY target_id ORDER BY id ASC) AS rn "
                  "FROM images "
                  "WHERE target_id IN ("
                  (clojure.string/join "," (repeat (count ids) "?")) ;; parameterized!
                  ") AND thumbnail = TRUE) t")]])
    (sql/where [:= :rn 1])))

(defn get-first-thumbnail-query [ids]
  (let [placeholders (clojure.string/join "," (repeat (count ids) "?"))

        _ (println ">o> abc.placeholders" placeholders)
        subquery
        (str "(SELECT id, target_id, thumbnail, filename, "
          "ROW_NUMBER() OVER (PARTITION BY target_id ORDER BY id ASC) AS rn "
          "FROM images "
          "WHERE target_id IN (" placeholders ") AND thumbnail = TRUE) t")]
    (-> (sql/select :id :target_id :thumbnail :filename)
      (sql/from [[:raw subquery]])
      (sql/where [:= :rn 1]))))


(defn get-first-thumbnail-query [ids]
  (let [placeholders (clojure.string/join "," (repeat (count ids) "?"))
        subquery
        (str "(SELECT id, target_id, thumbnail, filename, "
          "ROW_NUMBER() OVER (PARTITION BY target_id ORDER BY id ASC) AS rn "
          "FROM images "
          "WHERE target_id IN (" placeholders ") AND thumbnail = TRUE) t")]
    (-> (sql/select :id :target_id :thumbnail :filename)
      (sql/from [[:raw subquery]])
      (sql/where [:= :rn 1]))))


(defn get-one-thumbnail-query [tx id]
  (jdbc/execute-one! tx (-> (sql/select :id :target_id :thumbnail :filename)
                          (sql/from :images)
                          (sql/where [:and
                                      [:= :target_id id]
                                      [:= :thumbnail true]])
                          sql-format)))


(defn fetch-thumbnails-for-ids [tx ids]
  (vec (map #(get-one-thumbnail-query tx %) ids)))

(defn create-url [pool_id model_id type cover_image_id]
    (str "/inventory/" pool_id "/models/" model_id "/images/" cover_image_id))

(defn add-cover-image-urls [items res pool_id]
  (vec
    (map-indexed
      (fn [idx item]
        (let [
              cover-image-id (:cover_image_id item)
              origin_table (:origin_table item)
              ;res-id (get-in res [idx :id])
              ;res-id   (res.target_id == item.id ])
              res-entry (first (vec (filter #(= (:target_id %) (:id item)) res)))
              res-id (:id res-entry)
              p (println ">o> abc.res-entry" res-entry)
              p (println ">o> abc.res-id" res-id)

              ]
          (cond
            (and (= "models" origin_table) cover-image-id)
            (assoc item :cover_image_url (create-url pool_id (:id item) "images" cover-image-id))

            (and (= "models" origin_table) res-id )
            (assoc item :cover_image_url (create-url pool_id (:id item) "images" res-id))

            :else
            item)))
      items)))




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
                   (#(from-category tx % category_id))))




         ;; FIXME: remove this
         post-fnc (fn [items]
                    ;(println ">o> abc.items??" items)

                    ;extract all id's

                    (let [
                          ;ids (vec (keep :id items))


                          ids (->> items
                                (keep :id)
                                ;(take 2)
                                vec)

                          p (println ">o> abc.ids" ids)


                          res (vec (keep identity (fetch-thumbnails-for-ids tx ids)))

                          ;query (get-first-thumbnail-query ids)
                          p (println ">o> abc.res" res)

                          ; [sql-str & _] (sql-format query)
                          ;
                          ;;result (jdbc/execute! tx (-> query sql-format))
                          ;result   (jdbc/execute! tx (into [sql-str] ids))
                          ; p (println ">o> abc.result" result)


                          items (add-cover-image-urls items res pool_id )
p (println ">o> abc.items" items)
                          ]
                      items
                      )


                    ;; merge items.id==res.target_id if cover_image_id is null then cover_image_url="/inventory/images/{res.id}"

                    ;(mapv (fn [item]
                    ;        (-> item
                    ;          (assoc :cover_image_url
                    ;            "/inventory/8bd16d45-056d-5590-bc7f-12849f034351/models/847906e1-8e03-57bb-a4d5-bf68d70ab706/images/9c5284a4-c907-4944-9fe2-d29579126213")
                    ;          ;(assoc :cover_image_id
                    ;          ;  "9c5284a4-c907-4944-9fe2-d29579126213")
                    ;          ))
                    ;  items)


                    ;items
                    )
         ]
     (debug (sql-format query :inline true))

     (if (url-ends-with-uuid? (:uri request))
       (let [res (jdbc/execute-one! tx (-> query sql-format))]
         (if res
           (response res)
           (status 404)))
       (response (create-pagination-response request query with-pagination? post-fnc))))))

(defn get-models-of-pool-with-pagination-handler [request]
  (get-models-handler request true))

;###################################################################################

(defn create-model-handler-by-pool [request]
  (let [created_ts (LocalDateTime/now)
        model-id (get-in request [:path-params :model_id])
        pool-id (get-in request [:path-params :pool_id])
        body-params (:body-params request)
        tx (:tx request)
        model (assoc body-params :created_at created_ts :updated_at created_ts)
        categories (:category_ids model)
        model (dissoc model :category_ids)]
    (try
      (let [res (jdbc/execute-one! tx (-> (sql/insert-into :models)
                                        (sql/values [model])
                                        (sql/returning :*)
                                        sql-format))
            model-id (:id res)]
        (doseq [category-id categories]
          (jdbc/execute! tx (-> (sql/insert-into :model_links)
                              (sql/values [{:model_id model-id :model_group_id (to-uuid category-id)}])
                              sql-format))
          (jdbc/execute! tx (-> (sql/insert-into :inventory_pools_model_groups)
                              (sql/values [{:inventory_pool_id (to-uuid pool-id) :model_group_id (to-uuid category-id)}])
                              sql-format)))
        (if res
          (response [res])
          (bad-request {:error "Failed to create model"})))
      (catch Exception e
        (error "Failed to create model" e)
        (bad-request {:error "Failed to create model" :details (.getMessage e)})))))
