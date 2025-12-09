(ns leihs.inventory.client.actions
  (:require
   ["~/i18n.config.js" :as i18n :refer [i18n]]
   [leihs.inventory.client.lib.client :refer [http-client]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [promesa.core :as p]))

(defn profile [action]
  (p/let [request (.. action -request (formData))
          method (aget action "request" "method")]

    (case method
      "PATCH"
      (-> http-client
          (.patch "/inventory/profile/" request (cj {:cache
                                                     {:update {:profile "delete"}}}))
          (.then (fn [_]
                   (.. i18n (changeLanguage request))
                   #js {:status "ok"}))

          (.catch (fn [error]
                    (js/console.error "Language change error:" error)
                    #js {:error (.-message error)}))))))

(defn list-page [action]
  (p/let [form-data (.. action -request (formData))
          url (.get form-data "url")
          format (.get form-data "format")
          accept-header (case format
                          "csv" "text/csv"
                          "excel" "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                          "text/csv")]

    (-> http-client
        (.get url (cj {:cache false
                       :headers {:Accept accept-header}
                       :responseType "blob"}))
        (.then (fn [response]
                 (let [blob (.-data response)
                       url (js/URL.createObjectURL blob)
                       link (.createElement js/document "a")
                       content-disposition (.. response -headers (get "content-disposition"))
                       filename (second (re-find #"filename=\"(.+)\"" content-disposition))]
                   (set! (.-href link) url)
                   (set! (.-download link) filename)
                   (.appendChild (.-body js/document) link)
                   (.click link)
                   (.removeChild (.-body js/document) link)
                   (js/URL.revokeObjectURL url)
                   #js {:status "ok"})))
        (.catch (fn [error]
                  (js/console.error "Export error:" error)
                  #js {:error (.-message error)})))))
