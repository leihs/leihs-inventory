(ns leihs.inventory.server.resources.pool.list.helper
  (:import [java.time Instant]))

(defn prepare-filters [filters]
  (cond-> filters
    (contains? filters :retired)
    (assoc :retired (case (:retired filters)
                      true (Instant/now)
                      false nil
                      (:retired filters)))))

