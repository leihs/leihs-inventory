(ns leihs.inventory.client.routes.layout
  (:require
   ["@@/console-feed" :refer [LogsContainer]]
   ["@@/sonner" :refer [Toaster]]
   ["@@/tooltip" :refer [Tooltip TooltipTrigger TooltipContent TooltipProvider]]
   ["react-router-dom" :as router :refer [Outlet]]
   [leihs.inventory.client.routes.components.header :as header]
   [uix.core :as uix :refer [defui $]]
   [uix.dom]))

(defui layout []
  (let [{:keys [profile]} (router/useLoaderData)]

    ($ TooltipProvider
       ($ :<>
          ($ header/main profile)
          ($ :main {:className "container"}

             ($ :div {:className "fixed bg-white shadow-lg z-50 bottom-20 left-12 rounded-lg"}
                ($ LogsContainer))
             ($ Outlet)
             ($ Toaster {:position "top-center"
                         :closeButton true
                         :richColors true}))))))
