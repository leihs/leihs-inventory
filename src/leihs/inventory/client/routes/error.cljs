 (ns leihs.inventory.client.routes.error
   (:require
    ["react-i18next" :refer [useTranslation]]
    ["react-router-dom" :as router :refer [useRouteError]]
    [uix.core :as uix :refer [$]]))

 (defn page []
  (let [[t] (useTranslation)
        error (useRouteError)
        env (.. js/process -env -NODE_ENV)
        is-prod (= env "production")
        resp (.. error -response)
        data (or (when resp (.-data resp)) (.. error -data))
        reason (or (aget data "reason")
                   (aget data "detail")
                   (aget data "message")
                   (when (string? data) data)
                   (.. error -statusText))]

     (js/console.error "error" error)

     ($ :div {:class-name "w-screen h-screen flex flex-col items-center justify-center"}
        ($ :h1 {:className "text-2xl font-bold"}
           (or (when error (.. error -status))
               (when error (.. error -response -status))
               500))
        ($ :p reason)
        (when (not is-prod)
          ($ :pre {:class-name "text-left whitespace-pre-wrap text-sm bg-muted/40 rounded p-4 mt-4 overflow-auto max-h-[40vh]"}
             (or (.. error -stack)
                 (js/JSON.stringify error nil 2))))
        ($ :a {:href "/inventory/"
               :class-name "mt-12"}
           (t "notfound.back-to-home" "Zurück zur Startseite"))
        ($ :a {:href "/inventory"
               :class-name "mt-4"}
           (t "notfound.back-to-landing" "Back to Landingpage")))))
