(ns leihs.inventory.server.utils.export-result-set-stream
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as jdbc-rs])
  (:import [java.sql ResultSet]))

(def default-fetch-size 500)

(def ^:private rs-opts {:builder-fn jdbc-rs/as-unqualified-arrays})

(defn- rs->row
  [^ResultSet rs column-count]
  (mapv #(.getObject rs ^int %) (range 1 (inc column-count))))

(defn with-result-set-stream
  "Single PreparedStatement / ResultSet pass with server-side fetch size."
  [tx sql {:keys [fetch-size on-metadata on-row]}]
  (let [actual-fetch-size (or fetch-size default-fetch-size)]
    (jdbc/on-connection [conn tx]
                        (with-open [stmt (jdbc/prepare conn sql)]
                          (.setFetchSize stmt actual-fetch-size)
                          (let [^ResultSet rs (.executeQuery stmt)
                                rsmeta (.getMetaData rs)
                                column-labels (jdbc-rs/get-unqualified-column-names rsmeta rs-opts)
                                column-count (.getColumnCount rsmeta)]
                            (on-metadata column-labels)
                            (loop [more (.next rs)]
                              (when more
                                (on-row (rs->row rs column-count))
                                (recur (.next rs)))))))))
