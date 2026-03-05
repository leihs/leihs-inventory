(ns leihs.inventory.client.routes.pools.inventory.scan-edit.page
  (:require
   ["@@/card" :refer [Card CardContent CardHeader CardTitle]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :refer [useLoaderData]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defui page []
  (let [[t] (useTranslation)]

    ($ Card {:class-name "my-4"}
       ($ CardHeader
          ($ CardTitle (t "pool.models.tabs.scan_edit")))
       ($ CardContent
          ($ :section {:className "rounded-md border overflow-x-hidden p-4"}
             ($ :p "Scan & Edit - Coming soon..."))))))
