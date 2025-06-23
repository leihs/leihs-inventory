(ns leihs.inventory.client.lib.form-helper
  (:require
   [leihs.inventory.client.lib.client :refer [http-client]]))

(defn replace-nil-values [data]
  (cond
    (map? data)
    (into {}
          (for [[k v] data]
            [k (replace-nil-values (if (nil? v) "" v))]))

    (vector? data)
    (vec (map #(replace-nil-values (if (nil? %) "" %)) data))

    :else
    data))

(defn remove-nil-values [data]
  (cond
    (map? data)
    (into {}
          (for [[k v] data
                :when (some? v)]
            [k (remove-nil-values v)]))

    (vector? data)
    (vec (filter some? (map remove-nil-values data)))

    :else
    data))

(defn emtpy-string-to-nil [data]
  (cond
    (map? data)
    (into {}
          (for [[k v] data]
            (if (= v "")
              [k nil]
              [k v])))

    :else
    data))

(defn create-file-from-url [url name type]
  (js/Promise.
   (fn [resolve reject]
     (-> http-client
         (.get url #js {:headers #js {:Accept type}
                        :responseType "blob"})

         (.then (fn [response]
                  (let [blob (.-data response)
                        file (js/File. #js [blob] name #js {:type type})]
                    (resolve file))))

         (.catch (fn [error]
                   (reject error)))))))
