(ns leihs.inventory.client.actions
  (:require
   ["~/i18n.config.js" :as i18n :refer [i18n]]
   [clojure.string]
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
          (.catch (fn [error]
                    (js/console.error "Export error:" error)
                    #js {:error (.-message error)}))))))

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
          (.catch (fn [error]
                    (js/console.error "Serial number update error:" error)
                    #js {:error (.-message error)}))))))

(defn search-edit-page [action]
  (p/let [request (.-request action)
          json-data (.json request)
          form-values (jc json-data)

          ;; Extract pool-id from route params
          params (.. ^js action -params)
          pool-id (aget params "pool-id")

          method (aget action "request" "method")]

    (js/console.debug "Action received form values:" form-values)

    (case method
      "POST"
      (try
        (let [;; Build the filter_q query from form data
              query-encoded (js/encodeURIComponent form-values)]

          (-> http-client
              (.get (str "/inventory/" pool-id "/items/?filter_q=" query-encoded)
                    (cj {:cache false}))
              (.then (fn [response]
                       (js/console.log "Search results:" (.-data response))
                       #js {:status "ok"
                            :results (.-data response)}))
              (.catch (fn [error]
                        (js/console.error "Search error:" error)
                        (let [error-msg (if (.-response error)
                                          (or (.. error -response -data -message)
                                              (.. error -response -statusText)
                                              "Unknown server error")
                                          (.-message error))]
                          #js {:error error-msg})))))

        (catch js/Error e
          (js/console.error "Query building error:" e)
          (p/resolved #js {:error (str "Failed to build query: " (.-message e))}))))))

