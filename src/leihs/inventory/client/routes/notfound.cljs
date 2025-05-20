(ns leihs.inventory.client.routes.notfound
  (:require
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router :refer [useRouteError]]
   [uix.core :as uix :refer [$]]))

(defn page []
  (let [[t] (useTranslation)
        error (useRouteError)]

    (js/console.error "error" error)

    ($ :div {:class-name "w-screen h-screen flex flex-col items-center justify-center"}
       ($ :h1 {:className "text-2xl font-bold"} (.. error -status))
       ($ :p (.. error -statusText) ": " (.. error -data))
       ($ :a {:href "/inventory/"
              :class-name "mt-12"}
          (t "notfound.back-to-home" "Zurück zur Startseite")))))
