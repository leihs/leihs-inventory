(ns leihs.inventory.client.lib.form-helper
  (:require
   [leihs.inventory.client.lib.client :refer [http-client]]))

;; Replaces nil values with empty strings in maps or vectors, only at top level
(defn replace-nil-values [data]
  (cond
    (map? data)
    (into {}
          (for [[k v] data]
            [k (if (nil? v) "" v)]))

    (vector? data)
    (vec (map #(if (nil? %) "" %) data))

    :else
    data))

;; Takes a map or vector and replaces nil values with empty strings, only at the top level.
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
