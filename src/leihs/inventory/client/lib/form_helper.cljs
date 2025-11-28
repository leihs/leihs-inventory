(ns leihs.inventory.client.lib.form-helper
  (:require
   [leihs.inventory.client.lib.client :refer [http-client]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [promesa.core :as p]))

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

(defn process-files [data & file-keys]

  (if (seq file-keys)
    (p/let [processed-data
            (p/all
             (map (fn [file-key]
                    (let [items (get data file-key)]
                      (if (seq items)
                        (p/let [files (p/all
                                       (mapv (fn [item]
                                               (p/let [file (create-file-from-url
                                                             (:url item)
                                                             (:filename item)
                                                             (:content_type item))]
                                                 (merge {:id (:id item)
                                                         :file file}
                                                        (select-keys item [:is_cover]))))
                                             items))]
                          [file-key files])
                        (p/resolved [file-key items]))))
                  file-keys))]
      (cj (reduce (fn [acc [k v]]
                    (assoc acc k v))
                  data
                  processed-data)))
    (p/resolved (cj data))))
