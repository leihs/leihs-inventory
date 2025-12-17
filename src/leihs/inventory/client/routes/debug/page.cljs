(ns leihs.inventory.client.routes.debug.page
  (:require
   ["react-i18next" :refer [useTranslation]]
   [leihs.inventory.client.routes.components.translate-check :as translate-check]
   [leihs.inventory.client.routes.debug.components.error :as error]
   [leihs.inventory.client.routes.debug.components.lang-switcher :as lang-switcher]
   [uix.core :as uix :refer [defui $]]
   [uix.dom]))

(defui page []
  (let [[t] (useTranslation)]
    ($ :div.p-8
       ($ :h1.mt-16.text-2xl.font-bold (t "debug.welcome", "Willkommen"))
       ($ :p.mb-8 (t "debug.welcome-text" "Willkommen auf der Debug Seite. Hier gibt es ein paar nützliche Funktionalität"))

       ($ error/main)
       ($ translate-check/main)
       ($ lang-switcher/main))))

