(ns leihs.inventory.client.lib.debounce)

(defn main [func & {:keys [timeout] :or {timeout 1000}}]
  (let [timer (atom nil)]
    (fn [& args]
      (js/clearTimeout @timer)
      (reset! timer (js/setTimeout #(apply func args) timeout)))))
