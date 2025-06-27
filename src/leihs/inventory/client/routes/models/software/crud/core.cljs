(ns leihs.inventory.client.routes.models.software.crud.core
  (:require
   [leihs.inventory.client.lib.client :refer [http-client]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]))

(defn create-file-from-url [url name type]
  (js/Promise.
   (fn [resolve reject]
     (-> http-client
         (.get url #js {:headers #js {:Accept "image/*"}
                        :responseType "blob"})

         (.then (fn [response]
                  (let [blob (.-data response)
                        file (js/File. #js [blob] name #js {:type "image/*"})]
                    (resolve file))))

         (.catch (fn [error]
                   (reject error)))))))

(defn prepare-default-values [model]
  (let [images (:images model)
        attachments (:attachments model)]

    (-> (js/Promise.all
         (concat

          (when (seq images)
            (map (fn [image]
                   (let [id (:id image)
                         url (:url image)
                         filename (:filename image)
                         content-type (:content_type image)
                         is-cover (:is_cover image)]
                     (-> (create-file-from-url url filename content-type)
                         (.then (fn [file]
                                  {:id id
                                   :file file
                                   :is_cover is-cover}))
                         (.catch (fn [error]
                                   (js/console.error "Error processing image file" error)
                                   (js/Promise.reject error))))))
                 images))

          (when (seq attachments)
            (map (fn [attachment]
                   (let [id (:id attachment)
                         url (:url attachment)
                         filename (:filename attachment)
                         content-type (:content_type attachment)]

                     (-> (create-file-from-url url filename content-type)
                         (.then (fn [file]
                                  {:id id
                                   :file file}))
                         (.catch (fn [error]
                                   (js/console.error "Error processing attachment file" error)
                                   (js/Promise.reject error))))))
                 attachments))))
        (.then (fn [files]
                 (let [files-vec (vec files)
                       processed-images (filter #(contains? % :is_cover) files-vec)
                       processed-attachments (filter #(not (contains? % :is_cover)) files-vec)
                       model (cj (-> model
                                     (assoc :images processed-images)
                                     (assoc :attachments processed-attachments)))]

                   model)))
        (.catch (fn [error]
                  (js/console.error "Promise error" error)
                  (js/Promise.reject error))))))

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
