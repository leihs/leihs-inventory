(ns leihs.inventory.client.lib.utils)

(defn jc [js]
  (js->clj js {:keywordize-keys true}))

(defn cj [clj]
  (clj->js clj))
