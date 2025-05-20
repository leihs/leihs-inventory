(ns leihs.inventory.client.lib.debounce)

(defn debounce
  "Returns a debounced version of function `f` that waits `delay-ms` milliseconds
   after the last call before invoking `f`."
  [f delay-ms]
  (let [timeout-id (atom nil)
        last-args (atom nil)]
    (fn [& args]
      (reset! last-args args)
      (when @timeout-id
        (js/clearTimeout @timeout-id))
      (reset! timeout-id
              (js/setTimeout
               (fn []
                 (apply f @last-args))
               delay-ms)))))
