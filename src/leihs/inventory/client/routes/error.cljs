 (ns leihs.inventory.client.routes.error
   (:require
    ["react-i18next" :refer [useTranslation]]
    ["react-router-dom" :as router :refer [useRouteError]]
    [clojure.string :as str]
    [uix.core :as uix :refer [$]]))

 (defn page []
  (let [[t] (useTranslation)
        error (useRouteError)
        env (.. js/process -env -NODE_ENV)
        is-prod (= env "production")
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
        json? (and (some? data) (not (string? data)))
        reason (or (when json? (aget data "reason"))
                   (when json? (aget data "detail"))
                   (when json? (aget data "message"))
                   (when (and (string? data) (not is-html)) data)
                   status-text)
        display-msg (if is-html (str status " " (if (seq status-text) status-text "Not Found")) reason)]

    (js/console.error "error" error)

    ($ :div {:class-name "w-screen h-screen flex flex-col items-center justify-center"}
       ($ :h1 {:className "text-2xl font-bold"} status)
       ($ :p display-msg)
       (when (not is-prod)
         ($ :pre {:class-name "text-left whitespace-pre-wrap text-sm bg-muted/40 rounded p-4 mt-4 overflow-auto max-h-[40vh]"}
            (or (when error (.-stack error))
                (js/JSON.stringify (or error #js {}) nil 2))))
       ($ :a {:href "/inventory/"
              :class-name "mt-12"}
          (t "notfound.back-to-home" "Zurück zur Startseite"))
       )))
