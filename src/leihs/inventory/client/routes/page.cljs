(ns leihs.inventory.client.routes.page
  (:require
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :refer [Link]]
   [uix.core :as uix :refer [defui $]]
   [uix.dom]))

(defui page []
  (let [[t] (useTranslation)]

    ($ :div
       ($ :h1 {:class-name "text-2xl font-bold mt-12 mb-6"} "Welcome")
       ($ :p (t "root.welcome", "Welcome to Leihs Inventory!"))
       ($ :ul {:class-name "mt-6"}
          ($ :li ($ Link {:class-name "underline" :to "debug"} "Debug"))
          ($ :li ($ Link {:class-name "underline"
                          :to "8bd16d45-056d-5590-bc7f-12849f034351/models"}
                    "Ausleihe Toni - 8bd16d45-056d-5590-bc7f-12849f034351"))))))

