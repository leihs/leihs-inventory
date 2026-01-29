(ns leihs.inventory.client.routes.layout
  (:require
   ["@@/alert-dialog" :refer [AlertDialog AlertDialogContent
                              AlertDialogDescription AlertDialogHeader
                              AlertDialogTitle]]
   ["@@/sonner" :refer [Toaster]]
   ["@@/tooltip" :refer [TooltipProvider]]
   ["lucide-react" :refer [WifiOff]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router :refer [Outlet]]
   [leihs.inventory.client.lib.hooks :as hooks]
   [leihs.inventory.client.routes.components.header :as header]
   [leihs.inventory.client.routes.components.theme-provider :refer [ThemeProvider]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defui OfflineDialog []
  (let [[t] (useTranslation)
        network (hooks/use-network-state)]

    (when-not (.-online network)
      ($ AlertDialog {:open true}
         ($ AlertDialogContent
            ($ AlertDialogHeader
               ($ AlertDialogTitle {:as-child true}
                  ($ :h2 {:class-name "flex items-center gap-2"}
                     ($ WifiOff) (t "error.alerts.offline.title"))))

            ($ AlertDialogDescription (t "error.alerts.offline.description")))))))

(defui layout []
  (let [{:keys [profile]} (router/useLoaderData)]

    ($ ThemeProvider {:default-theme "system"}
       ($ TooltipProvider
          ($ :<>
             ($ header/main profile)
             ($ :main {:className "md:container"}
                ($ Outlet)
                ($ Toaster {:position "top-center"
                            :closeButton true
                            :richColors true}))
             ($ OfflineDialog))))))
