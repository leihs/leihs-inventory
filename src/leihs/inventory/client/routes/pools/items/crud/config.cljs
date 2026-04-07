(ns leihs.inventory.client.routes.pools.items.crud.config
  (:require
   ["zod" :as z]
   [leihs.inventory.client.lib.utils :refer [cj]]))

(def entities
  {:item
   {:translation-namespace "pool.items.item"
    :features {:batch-creation true
               :copy-from-existing true}
    :groups ["Mandatory data" "Status" "Inventory" "Eigenschaften"
             "General Information" "Location" "Invoice Information"]
    :custom-fields (fn [{:keys [is-create]}]
                     ;; Items: batch creation count field
                     (when is-create
                       [{:id "count"
                         :component "input"
                         :group "Mandatory data"
                         :position 0
                         :required true
                         :default 1
                         :props {:type "number"
                                 :min 0
                                 :max 999999
                                 :step 1}
                         :validator (-> (.. z -coerce (number))
                                        (.min 0)
                                        (.max 999999)
                                        (.int))}]))}

   :package
   {:translation-namespace "pool.packages.package"
    :features {:batch-creation false
               :copy-from-existing false
               :auto-retire-on-empty true}
    :groups ["Package" "Content" "Status" "Inventory"
             "General Information" "Location" "Invoice Information"]
    :custom-fields (fn [{:keys [is-create items]}]
                     ;; Packages: item selection field
                     [{:id "item_ids"
                       :type "array"
                       :component "items"
                       :group "Content"
                       :required true
                       :default (or items [])
                       :props {:text {:select "pool.packages.package.fields.items.select"
                                      :search "pool.packages.package.fields.items.search"
                                      :searching "pool.packages.package.fields.items.searching"
                                      :search_empty "pool.packages.package.fields.items.search_empty"
                                      :not_found "pool.packages.package.fields.items.not_found"}}
                       :validator (if is-create
                                    (-> (z/array (z/object (cj {:id (z/guid)})))
                                        (.min 1)
                                        (.transform (fn [arr] (mapv (fn [item] (.-id item)) arr))))
                                    (-> (z/array (z/object (cj {:id (z/guid)})))
                                        (.transform (fn [arr] (mapv (fn [item] (.-id item)) arr)))))}])}

   :license
   {:translation-namespace "pool.licenses.license"
    :features {:batch-creation false
               :copy-from-existing true
               :auto-assign-general-room true}
    :groups ["Mandatory data" "Status" "Invoice Information"
             "General Information" "Inventory" "Maintenance"]
    :custom-fields (fn [_context]
                     ;; Licenses: no custom fields
                     nil)}})
