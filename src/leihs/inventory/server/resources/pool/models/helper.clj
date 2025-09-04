(ns leihs.inventory.server.resources.pool.models.helper)

(defn normalize-model-data
  [data]
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
                                  (if-let [val (get data original-key)]
                                    (assoc acc db-key val)
                                    acc))
                                {}
                                key-map)]
    normalized-data))
