(ns leihs.inventory.client.routes.pools.software.crud.core
  (:require
   [leihs.inventory.client.lib.client :refer [http-client]]
   [leihs.inventory.client.lib.form-helper :as form-helper]
   [leihs.inventory.client.lib.utils :refer [cj jc]]))

(def default-values {:product ""
                     :version ""
                     :manufacturer ""
                     :technical_detail ""})

(defn prepare-default-values [model]
  (let [attachments (:attachments model)
        model (merge default-values
                     (form-helper/replace-nil-values model))]

    (-> (js/Promise.all
         (if (seq attachments)
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
                attachments)
           []))
        (.then (fn [files]
                 (let [model (cj (-> model
                                     (assoc :attachments files)))]

                   model)))
        (.catch (fn [error]
                  (js/console.error "Promise error" error)
                  (js/Promise.reject error))))))
