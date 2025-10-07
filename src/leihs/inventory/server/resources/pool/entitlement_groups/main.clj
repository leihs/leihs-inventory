(ns leihs.inventory.server.resources.pool.entitlement-groups.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.helper :refer [log-by-severity]]
   [leihs.inventory.server.utils.request-utils :refer [path-params]]
   ;[next.jdbc.sql :as jdbc]
   [next.jdbc :as jdbc]
   ;[java.util UUID]
   [ring.middleware.accept]
   [taoensso.timbre :refer [debug error]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [ring.util.response :refer [response ]])

  ;)

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
    (nil? x)    nil

    (uuid? x) x

    (string? x)     (UUID/fromString x)

    (sequential? x)     (mapv to-uuid x)

    :else    (throw (ex-info "Unsupported type for uuid conversion"
             {:value x
              :type (type x)}))))

(def ERROR_GET "Failed to get entitlement-groups")

(defn merge-by-id
  "Merge two vectors of maps by matching :id.
   Fields from v2 override those from v1 on collision."
  [v1 v2]
  (let [m2 (into {} (map (juxt :id identity) v2))]
    (mapv #(merge % (get m2 (:id %))) v1)))

(defn index-resources [request]
      (try
        (let [tx (:tx request)
              pool_id (-> request path-params :pool_id)
              query (-> (sql/select :g.*)
                        (sql/from [:entitlement_groups :g])
                        (sql/join [:inventory_pools :ip] [:= :g.inventory_pool_id :ip.id])
                        (cond-> pool_id (sql/where [:= :g.inventory_pool_id pool_id]))
                        (sql/order-by :g.name))
              post-fnc (fn [models]
                         (let [ids (to-uuid (mapv :id models))
                               result (jdbc/execute! tx (prep-query ids))]
                           (merge-by-id models result)))]
          (response (create-pagination-response request query nil post-fnc)))
        (catch Exception e
          (exception-handler request ERROR_GET e))))
