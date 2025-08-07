(ns leihs.inventory.client.routes.pools.items.crud.core
  (:require
   [leihs.inventory.client.lib.form-helper :as form-helper]
   [leihs.inventory.client.lib.utils :refer [cj jc]]))

(def default-values {:product ""
                     :is_package false
                     :manufacturer ""
                     :description ""
                     :internal_description ""
                     :technical_detail ""
                     :hand_over_note ""
                     :version ""
                     :categories []
                     :entitlements []
                     :properties []
                     :accessories []})

(defn prepare-default-values [model]
  (let [images (:images model)
        attachments (:attachments model)
        model (merge default-values
                     (form-helper/replace-nil-values model))]

    (-> (js/Promise.all
         (concat

          (when (seq images)
            (map (fn [image]
                   (let [id (:id image)
                         url (:url image)
                         filename (:filename image)
                         content-type (:content_type image)
                         is-cover (:is_cover image)]
                     (-> (form-helper/create-file-from-url url filename content-type)
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

                     (-> (form-helper/create-file-from-url url filename content-type)
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

