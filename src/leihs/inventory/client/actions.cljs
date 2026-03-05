(ns leihs.inventory.client.actions
  (:require
   ["~/i18n.config.js" :as i18n :refer [i18n]]
   [clojure.string]
   [leihs.inventory.client.lib.client :refer [http-client]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [promesa.core :as p]))

(defn- handle-error [err]
  (let [status (some-> err .-response .-status)]
    #js {:status "error"
         :httpStatus (or status 0)
         :message (.-message err)}))

(defn debug-action-error [action]
  (p/let [form-data (.. action -request (formData))
          error-type (.get form-data "error-type")
          method (aget action "request" "method")]
    (case method
      "POST"
      (case error-type
        "not-found"
        (-> http-client
            (.get "/inventory/non-existent-debug-endpoint")
            (.then (fn [_] #js {:status "ok"}))
            (.catch handle-error))

        "bad-request"
        (handle-error #js {:response #js {:status 400}
                           :message "Bad Request"})))))

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
          (.catch handle-error)))))

(defn export [action]
  (p/let [form-data (.. action -request (formData))
          url (.get form-data "url")
          format (.get form-data "format")
          accept-header (case format
                          "csv" "text/csv"
                          "excel" "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                          "text/csv")
          method (aget action "request" "method")]

    (case method
      "POST"
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
          (.catch handle-error)))))

(defn items-review-page [action]
  (p/let [form-data (.. action -request (formData))
          item-id (.get form-data "item-id")
          pool-id (.get form-data "pool-id")
          serial-number (.get form-data "serial_number")
          method (aget action "request" "method")]

    (case method
      "PATCH"
      (-> http-client
          (.patch (str "/inventory/" pool-id "/items/" item-id)
                  (js/JSON.stringify (cj {:serial_number serial-number}))
                  (cj {:cache false}))
          (.then (fn [_]
                   #js {:status "ok"}))
          (.catch handle-error)))))

(defn search-edit-page [action]
  (p/let [request (.-request action)
          json-data (.json request)
          body (jc json-data)
          params (.. ^js action -params)
          pool-id (aget params "pool-id")
          method (aget action "request" "method")]

    (case method
      "PATCH"
      (let [items (:selected-items body)
            data (cj (apply merge (:update body)))]
        (-> (p/all
             (map (fn [item-id]
                    (-> http-client
                        (.patch (str "/inventory/" pool-id "/items/" item-id)
                                (js/JSON.stringify data)
                                (cj {:cache false}))))
                  items))
            (.then (fn [_] #js {:status "ok"}))
            (.catch (fn [error]
                      (js/console.error "Bulk update error:" error)
                      #js {:error (.-message error)})))))))

