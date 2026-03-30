(ns leihs.inventory.client.routes.pools.items.config)

(def configs
  {:item
   {:translation-namespace "pool.items.item"
    :features {:batch-creation true
               :copy-from-existing true
               :package-membership-alert true}}

   :package
   {:translation-namespace "pool.packages.package"
    :features {:batch-creation false
               :copy-from-existing false
               :package-membership-alert false
               :auto-retire-on-empty true}}

   :license
   {:translation-namespace "pool.items.item"
    :features {:batch-creation false
               :copy-from-existing false
               :package-membership-alert false
               :auto-assign-general-room true}}})

(defn get-config [entity-type-keyword]
  (get configs entity-type-keyword))

(defn determine-entity-type
  "Determines entity type from route params"
  [params]
  (cond
    (aget params "package-id") :package
    (aget params "license-id") :license
    :else :item))
