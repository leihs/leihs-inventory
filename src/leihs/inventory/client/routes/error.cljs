(ns leihs.inventory.client.routes.error
  (:require
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router :refer [useRouteError]]
   ["@/env.js" :refer [NODE_ENV INVENTORY_ERROR_FRIENDLY_MESSAGE]]
   [clojure.string :as str]
   [uix.core :as uix :refer [$]]))

 (defn page []
  (let [[t] (useTranslation)
        error (useRouteError)
        is-prod (= NODE_ENV "production")
        friendly-msg (when (seq INVENTORY_ERROR_FRIENDLY_MESSAGE) INVENTORY_ERROR_FRIENDLY_MESSAGE)
        resp (when error (.-response error))
        headers (when resp (.-headers resp))
        content-type (or (when headers (aget headers "content-type"))
                         (when headers (aget headers "Content-Type")))
        data (or (when resp (.-data resp)) (when error (.-data error)))
        status (or (when error (.-status error)) (when resp (.-status resp)) 404)
        status-text (or (when error (.-statusText error)) (when resp (.-statusText resp)) "")
        is-html (or (and content-type (str/includes? (str/lower-case content-type) "text/html"))
                    (and (string? data) (or (str/starts-with? data "<!DOCTYPE")
                                            (str/starts-with? data "<html"))))
        json? (and (some? data) (not (string? data)) (not is-html))
        reason (or (when json? (aget data "reason"))
                   (when json? (aget data "detail"))
                   (when json? (aget data "message"))
                   (when (and (string? data) (not is-html)) data)
                   status-text)
        display-msg (if is-html (str status " " (if (seq status-text) status-text "Not Found")) reason)
        fallback-msg (cond
                       (= status 404) "Not Found"
                       (= status 401) "Unauthorized"
                       (= status 403) "Forbidden"
                       (= status 422) "Unprocessable Entity"
                       (= status 500) "Internal Server Error"
                       :else (if (seq status-text) status-text "Error"))
        final-msg (if (and display-msg (seq display-msg)) display-msg fallback-msg)]

    (js/console.error "error" error)

    ($ :div {:class-name "w-screen h-screen flex flex-col items-center justify-center"}
       ($ :h1 {:className "text-2xl font-bold"} status)
       ($ :p final-msg)
       (when friendly-msg
         ($ :p {:class-name "text-muted-foreground"} friendly-msg))
       (let [debug-str (or (when error (.-stack error))
                           (when error (js/JSON.stringify error nil 2)))]
         (when (and (not is-prod) debug-str (seq debug-str))
           ($ :pre {:class-name "text-left whitespace-pre-wrap text-sm bg-muted/40 rounded p-4 mt-4 overflow-auto max-h-[40vh]"}
              debug-str)))
       ($ :a {:href "/inventory/"
              :class-name "mt-12"}
          (t "notfound.back-to-home" "Back to Home"))
       )))
