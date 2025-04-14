(ns leihs.inventory.server.resources.models.queries
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.core :refer [single-entity-get-request?]]
   [leihs.inventory.server.utils.pagination :refer [create-paginated-response fetch-pagination-params]]
   [next.jdbc.sql :as jdbc]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]])
  (:import (java.time LocalDateTime)
           (java.util UUID)))

(defn item-query [query item-id]
  (-> query
      (cond-> item-id
        (sql/where [:= :i.id item-id]))))

(defn entitlements-query [query entitlement-id]
  (-> query
      (sql/select-distinct [:e.*])
      (sql/join [:entitlements :e] [:= :m.id :e.model_id])
      (cond-> entitlement-id
        (sql/where [:= :e.id entitlement-id]))))

(defn model-links-query [query model-links-id pool_id]
  (-> query
      (sql/select-distinct [:ml.*])
      (cond-> (nil? pool_id) (sql/join [:model_links :ml] [:= :m.id :ml.model_id]))
      (cond-> model-links-id
        (sql/where [:= :ml.id model-links-id]))))

(defn properties-query [query properties-id]
  (-> query
      (sql/select-distinct [:p.*])
      (sql/join [:properties :p] [:= :m.id :p.model_id])
      (cond-> properties-id
        (sql/where [:= :p.id properties-id]))))

(defn accessories-query
  ([query pool-id model-id accessories-id]
   (accessories-query query accessories-id "n/d"))
  ([query pool-id model-id accessories-id type]
   (-> query
       (sql/select-distinct [:a.*])
       (sql/join [:accessories :a] [:= :m.id :a.model_id])
       (sql/where [:= :models.id model-id])
       (cond-> accessories-id
         (sql/where [:= :a.id accessories-id])))))

(defn attachments-query
  ([query attachment-id]
   (attachments-query query attachment-id "n/d"))
  ([query attachment-id type]
   (-> query
       (sql/select-distinct :a.id :a.content :a.filename :a.item_id)
       (sql/join [:attachments :a] [:= :m.id :a.model_id])
       (cond-> attachment-id
         (sql/where [:= :a.id attachment-id])))))

(defn base-pool-query [query pool-id type]
  (-> query
      (cond->
       pool-id (sql/left-join [:model_links :ml] [:= :m.id :ml.model_id])
       pool-id (sql/left-join [:model_groups :mg] [:= :mg.id :ml.model_group_id])
       pool-id (sql/left-join [:inventory_pools_model_groups :ipmg] [:= :mg.id :ipmg.model_group_id])
       pool-id (sql/left-join [:inventory_pools :ip] [:= :ip.id :ipmg.inventory_pool_id])
       pool-id (sql/where [:= :ip.id [:cast pool-id :uuid]]))))
