(ns leihs.inventory.server.resources.pool.entitlement-groups.entitlement-group.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.helper :refer [log-by-severity]]
   [leihs.inventory.server.utils.request-utils :refer [path-params]]
   ;[next.jdbc.sql :as jdbc]
   ;[java.util UUID]
   [next.jdbc :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [response]]
   [taoensso.timbre :refer [debug error]])
(:import
 [java.util UUID]))

;; FIXME: 2 is buggy
;(defn prep-query [ids]
;          (-> (sql/select
;                :g.name
;                :g.is_verification_required
;                [[:coalesce :m.number_of_models 0] :number_of_models])
;
;            (sql/from [:entitlement_groups :g])
;
;            ;; 1️⃣ Models subquery
;            (sql/left-join
;              [(-> (sql/select
;                     :entitlement_group_id
;                     [[:count :*] :number_of_models])
;                 (sql/from :entitlements)
;                 (sql/group-by :entitlement_group_id))
;               :m]
;              [:= :m.entitlement_group_id :g.id])
;
;            ;; 2️⃣ Users + Direct Users subquery (bug workaround)
;            (sql/left-join
;              [(-> (sql/select
;                     :entitlement_group_id
;                     [[:count :*] :number_of_users]
;                     [[:sum
;                       [:case
;                        [:when
;                         [:or
;                          [:= :type "direct_entitlement"]
;                          [:= :type "mixed"]]
;                         1]
;                        :else 0]]
;                      :number_of_direct_users])
;                 (sql/from :entitlement_groups_user)
;                 (sql/group-by :entitlement_group_id))
;               :u]
;              [:= :u.entitlement_group_id :g.id])
;
;            (sql/limit 1)
;
;            sql-format))



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
       (->> ids
         (map #(str "'" % "'"))
         (clojure.string/join ", "))
       ");")}))



(defn to-uuid [x]
  (cond
    (nil? x) nil

    (uuid? x) x

    (string? x) (UUID/fromString x)

    (sequential? x) (mapv to-uuid x)

    :else (throw (ex-info "Unsupported type for uuid conversion"
                   {:value x
                    :type (type x)}))))

(def ERROR_GET "Failed to get entitlement-groups")

(defn merge-by-id
  "Merge two vectors of maps by matching :id.
   Fields from v2 override those from v1 on collision."
  [v1 v2]
  (let [m2 (into {} (map (juxt :id identity) v2))]
    (mapv #(merge % (get m2 (:id %))) v1)))


(defn get-resource [request]
  (try
    (let [tx (:tx request)
          pool_id (-> request path-params :pool_id)
          entitlement-group-id (-> request path-params :entitlement_group_id)
          query (-> (sql/select :g.id, :g.name, :g.is_verification_required)
                  (sql/from [:entitlement_groups :g])
                  (sql/join [:inventory_pools :ip] [:= :g.inventory_pool_id :ip.id])
                  (sql/where [:= :g.inventory_pool_id pool_id])
                  (sql/where [:= :g.id entitlement-group-id])
                  (sql/order-by :g.name)
                  sql-format
                  )
          entitlement_group (jdbc/execute-one! tx query)



          query (-> (sql/select :egu.id
                      :egu.type
                      :u.firstname
                      :u.lastname
                      :u.email
                      :u.searchable)
                  (sql/from [:entitlement_groups_users :egu])
                  (sql/join [:users :u] [:= :egu.user_id :u.id])
                  (sql/where [:= :egu.entitlement_group_id entitlement-group-id])
                  sql-format)
          users-groups (jdbc/execute! tx query)

          ;p (println ">o> abc.users-groups" users-groups)

          groups (filter #(#{"group_entitlement"}
                        (:type %))
                users-groups)
          users (filter #(#{"direct_entitlement"}
                        (:type %))
                users-groups)




          query (-> (sql/select
                      :egg.id
                      :egg.group_id
                      :g.name
                      :g.searchable
                      )
                  (sql/from [:entitlement_groups_groups :egg])
                  (sql/join [:groups :g] [:= :egg.group_id :g.id])
                  (sql/where [:= :egg.entitlement_group_id entitlement-group-id])
                  sql-format)
          groups (jdbc/execute! tx query)


          result {
                  :entitlement-group entitlement_group
                  :users users
                  :groups groups
                  :models []
                  }
          ]

      (response result))
    (catch Exception e
      (exception-handler request ERROR_GET e))))
