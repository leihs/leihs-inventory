(ns leihs.inventory.server.resources.pool.models.helper)

(defn normalize-model-data
  ([data]
   (normalize-model-data data false))
  ([data preserve-nil-values?]
   (let [key-map {:type :type
                  :manufacturer :manufacturer
                  :product :product
                  :version :version
                  :is_package :is_package
                  :description :description
                  :technical_detail :technical_detail
                  :internal_description :internal_description
                  :hand_over_note :hand_over_note}
         normalized-data (reduce (fn [acc [db-key original-key]]
                                   (let [field-value (get data original-key)]
                                     (if (or (and preserve-nil-values? (contains? data original-key))
                                             (some? field-value))
                                       (assoc acc db-key field-value)
                                       acc)))
                                 {}
                                 key-map)]
     normalized-data)))
