(ns leihs.inventory.client.routes.components.translate-check
  (:require
   ["@@/button" :refer [Button]]
   ["react-i18next" :refer [useTranslation]]
   ["translation-check" :refer [showTranslations]]
   ["~/i18n.config.js" :as i18next :refer [i18n]]
   [uix.core :as uix :refer [defui $]]
   [uix.dom]))

(defui main []
  (let [[t] (useTranslation)]
    ($ :div
       {:class-name "mt-8 left-0"}
       ($ :h2 {:class-name "text-lg font-bold mb-4"} "Open Translation Check")
       ($ Button
          {:type "button" :on-click (fn [] (showTranslations i18n))}
          (t "translate-check.open" "Open Translation Check")))))
