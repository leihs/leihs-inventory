(ns leihs.inventory.server.resources.pool.entitlement-groups.entitlement-group.main
  (:require
   [clojure.set]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.request-utils :refer [path-params body-params]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [response]]
   [taoensso.timbre :refer [debug error]])
  (:import [java.util UUID]))

;; -----------------------------
;; Utility functions
;; -----------------------------

(defn to-uuid [x]
  (cond
    (nil? x) nil
    (uuid? x) x
    (string? x) (UUID/fromString x)
    (sequential? x) (mapv to-uuid x)
    :else (throw (ex-info "Unsupported type for uuid conversion"
                   {:value x :type (type x)}))))

(def ERROR_GET "Failed to get entitlement-groups")

(defn merge-by-id
  "Merge two vectors of maps by matching :id.
   Fields from v2 override those from v1 on collision."
  [v1 v2]
  (let [m2 (into {} (map (juxt :id identity) v2))]
    (mapv #(merge % (get m2 (:id %))) v1)))

(defn join-by
  "Left-join two collections of maps by shared key k.
   Keeps all maps from coll-a, merging matches from coll-b."
  [k coll-a coll-b]
  (let [b-index (into {} (map (juxt k identity)) coll-b)]
    (mapv #(merge % (get b-index (get % k))) coll-a)))

(defn add-allocation-considered-count
  "Adds computed fields:
   :available_count = items_count - allocations_in_other_entitlement_groups
   :is_quantity_ok = quantity <= available_count
   Also removes some transient keys."
  [entitlements]
  (mapv (fn [e]
          (let [available (- (:items_count e 0)
                            (:allocations_in_other_entitlement_groups e 0))
                e (assoc e
                    :available_count available
                    :is_quantity_ok (<= (:quantity e 0) available))]
            (dissoc e :entitlement_group_id :allocations_in_other_entitlement_groups)))
    entitlements))

;; -----------------------------
;; Queries
;; -----------------------------

(defn prep-query [ids]
  (sql-format
    {:raw
     (str
       "SELECT
            eg.id,
            eg.name,
            eg.is_verification_required,
            COALESCE(m.number_of_models, 0) AS number_of_models,
            COALESCE(u.number_of_users, 0) AS number_of_users,
            COALESCE(u.number_of_direct_users, 0) AS number_of_direct_users,
            COALESCE(g.number_of_groups, 0) AS number_of_groups
         FROM entitlement_groups eg
         LEFT JOIN (
             SELECT entitlement_group_id, COUNT(id) AS number_of_models
             FROM entitlements
             GROUP BY entitlement_group_id
         ) m ON m.entitlement_group_id = eg.id
         LEFT JOIN (
             SELECT entitlement_group_id,
                    COUNT(id) AS number_of_users,
                    SUM(CASE WHEN type IN ('direct_entitlement', 'mixed')
                             THEN 1 ELSE 0 END) AS number_of_direct_users
             FROM entitlement_groups_users
             GROUP BY entitlement_group_id
         ) u ON u.entitlement_group_id = eg.id
         LEFT JOIN (
             SELECT entitlement_group_id, COUNT(id) AS number_of_groups
             FROM entitlement_groups_groups
             GROUP BY entitlement_group_id
         ) g ON g.entitlement_group_id = eg.id
         WHERE eg.id IN ("
       (->> ids (map #(str "'" % "'")) (str/join ", "))
       ");")}))

(defn select-entitlements-with-item-count
  "Selects entitlements with corresponding item counts for given pool and models."
  [ds inventory-pool-id model-ids exclude-group-id]
  (let [subquery
        {:select [[[:count :*] :count]]
         :from [[:items :i]]
         :where [:and
                 [:= :i.model_id :e.model_id]
                 [:= :i.inventory_pool_id :eg.inventory_pool_id]
                 [:is :i.retired nil]
                 [:= :i.is_borrowable true]
                 [:is :i.parent_id nil]]}

        query (-> (sql/select
                    :e.model_id
                    [:e.quantity :allocations_in_other_entitlement_groups]
                    [[subquery] :items_count])
                (sql/from [:entitlements :e])
                (sql/join [:entitlement_groups :eg]
                  [:= :eg.id :e.entitlement_group_id])
                (sql/where [:and
                            [:= :eg.inventory_pool_id inventory-pool-id]
                            [:in :e.model_id model-ids]
                            [:!= :e.entitlement_group_id exclude-group-id]])
                sql-format)]
    (jdbc/execute! ds query)))

;; -----------------------------
;; HTTP Resource Handler
;; -----------------------------

(defn get-resource [request]
  (try
    (let [tx (:tx request)
          pool-id (-> request path-params :pool_id)
          entitlement-group-id (-> request path-params :entitlement_group_id)

          ;; 1️⃣ Fetch entitlement group
          query (-> (sql/select :g.id :g.name :g.is_verification_required)
                  (sql/from [:entitlement_groups :g])
                  (sql/join [:inventory_pools :ip] [:= :g.inventory_pool_id :ip.id])
                  (sql/where [:and
                              [:= :g.inventory_pool_id pool-id]
                              [:= :g.id entitlement-group-id]])
                  (sql/order-by :g.name)
                  sql-format)
          entitlement-group (jdbc/execute-one! tx query)

          ;; 2️⃣ Fetch users in entitlement group
          query (-> (sql/select :egu.id :egu.type :u.firstname :u.lastname :u.email :u.searchable)
                  (sql/from [:entitlement_groups_users :egu])
                  (sql/join [:users :u] [:= :egu.user_id :u.id])
                  (sql/where [:= :egu.entitlement_group_id entitlement-group-id])
                  sql-format)
          users-groups (jdbc/execute! tx query)

          ;; 3️⃣ Fetch models linked to entitlement group
          query (-> (sql/select
                      [:m.id :model_id]
                      :m.name
                      :e.id
                      :e.entitlement_group_id
                      :e.quantity)
                  (sql/from [:entitlements :e])
                  (sql/join [:models :m] [:= :e.model_id :m.id])
                  (sql/where [:= :e.entitlement_group_id entitlement-group-id])
                  sql-format)
          models (jdbc/execute! tx query)
          model-ids (mapv :model_id models)

          ;; 4️⃣ Fetch item counts per model
          model-ids (to-uuid model-ids)
          models2 (select-entitlements-with-item-count tx pool-id model-ids entitlement-group-id)

          ;; 5️⃣ Join models + counts, compute availability
          models3 (->> (join-by :model_id models models2)
                    add-allocation-considered-count)

          ;; 6️⃣ Fetch linked groups
          query (-> (sql/select :egg.id :egg.group_id :g.name :g.searchable)
                  (sql/from [:entitlement_groups_groups :egg])
                  (sql/join [:groups :g] [:= :egg.group_id :g.id])
                  (sql/where [:= :egg.entitlement_group_id entitlement-group-id])
                  sql-format)
          groups (jdbc/execute! tx query)

          result {:entitlement-group entitlement-group
                  :users users-groups
                  :groups groups
                  :models models3}]

      (response result))

    (catch Exception e
      (error e "Error fetching entitlement group")
      (exception-handler request ERROR_GET e))))




(defn put-resource [request]
  (try
    (let [tx (:tx request)
          pool_id (-> request path-params :pool_id)
          entitlement_group_id (-> request path-params :entitlement_group_id)
          data (-> request body-params)
          models (:models data)

          current-time (java.sql.Timestamp/from          (java.time.Instant/now)
                         )
          ;new-eg (-> data
          ;         (assoc :inventory_pool_id (to-uuid pool_id) :created_at current-time :updated_at current-time)
          ;         (dissoc :models)
          ;         )

          new-eg (-> data
                   ;(assoc :inventory_pool_id (to-uuid pool_id) :created_at current-time :updated_at current-time)
                   (dissoc :models)
                   )


          p (println ">o> abc.post1" pool_id new-eg)
          p (println ">o> abc.post2"  (:inventory_pool_id new-eg) (type (:inventory_pool_id new-eg)))
          p (println ">o> abc.post3"  (:is_verification_required new-eg) (type (:is_verification_required new-eg)))


          p (println ">o> abc.data-to-update" data)

          eg-data (select-keys data [:name :is_verification_required])
          p (println ">o> abc.eg-data" eg-data)
          query (-> (sql/update :entitlement_groups)
                  (sql/set eg-data)

                  ;(sql/values [new-eg])
                  ;(sql/where [:and [:= :id (to-uuid entitlement_group_id)] [:= :inventory_pool_id (to-uuid pool_id)]])
                  (sql/where [:= :id (to-uuid entitlement_group_id)])
                  (sql/returning :*)
                  sql-format
                  )
          res (jdbc/execute-one! tx query)
          p (println ">o> abc.res" res)

          ;; CREATE models-refs




          p (println ">o> abc.data" data)
          query (-> (sql/select :e.id, :e.model_id, :e.quantity)
                  (sql/from [:entitlements :e])
                  ;(sql/values new-models)
                  (sql/where [:= :e.entitlement_group_id (to-uuid entitlement_group_id)])
                  ;(sql/where [:and [:= :entitlement_group_id (to-uuid entitlement_group_id)][:= :inventory_pool_id (to-uuid pool_id)]])
                  ;(sql/returning :*)
                  sql-format
                  )
          db-models (jdbc/execute! tx query)

          ;p (println ">o> abc.db-models" db-models)
          ;db-model-ids (vector (filterv :id db-models))
          db-model-ids (filterv :id db-models)
          db-model-ids  (mapv :id db-model-ids)
          p (println ">o> abc.db-model-ids" db-model-ids)

          ;; update
          model-ids-to-update (filterv :id models)
          p (println ">o> abc.models-to-update" model-ids-to-update)

          ;; create
          new-models (vec (remove :id models))
          p (println ">o> abc.models-to-create" new-models)



          ;entitlement-ids (select-keys db-model-ids [:id])
          entitlement-ids (mapv :id models)
          p (println ">o> abc.entitlement-ids" entitlement-ids)


          ;; DELETE
          entitlement-ids-to-delete (remove (set entitlement-ids) db-model-ids)
          p (println ">o> abc.entitlement-ids-to-delete" entitlement-ids-to-delete)

          _ (when (seq entitlement-ids-to-delete) (jdbc/execute! tx (-> (sql/delete-from :entitlements)
                              (sql/where [:in :id entitlement-ids-to-delete] )
                              sql-format)))



          ;;models []
          ;p (println ">o> abc.res" res)
          ;entitlement-group-id (:id res)
          ;p (println ">o> abc.entitlement-group-id" entitlement-group-id)
          ;
          ;new-models (mapv (fn [item]
          ;                   (assoc item :entitlement_group_id entitlement-group-id
          ;                     ;:position 0
          ;                     ))
          ;             models)
          ;
          ;p (println ">o> abc.new-models" new-models)
          ;
          ;query (-> (sql/insert-into :entitlements)
          ;        (sql/values new-models)
          ;        (sql/returning :*)
          ;        sql-format
          ;        )
          ;models (jdbc/execute! tx query)
          ;p (println ">o> abc.new-models2" models)


          ]
      ;(response (create-pagination-response request query nil post-fnc)))
      (response {:entitlement_group res
                 ;:models models

                 }))
    (catch Exception e
      (println e)
      (exception-handler request ERROR_GET e))))