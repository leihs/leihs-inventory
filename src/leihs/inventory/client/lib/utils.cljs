(ns leihs.inventory.client.lib.utils
  (:require
   [leihs.core.core :refer [detect]]))

(defn jc [js]
  (js->clj js {:keywordize-keys true}))

(defn cj [clj]
  (clj->js clj))

(defn pool-read-only?
  [{:keys [permission]}]
  (= permission "read"))

(defn current-pool
  [pool-id profile]
  (->> profile :available_inventory_pools (detect #(= (:id %) pool-id))))

