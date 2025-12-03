(ns leihs.inventory.client.routes.layout
  (:require
   ["@@/sonner" :refer [Toaster]]
   ["@@/error-boundary" :refer [ErrorBoundary]]
   ["@@/tooltip" :refer [Tooltip TooltipTrigger TooltipContent TooltipProvider]]
   ["react-router-dom" :as router :refer [Outlet]]
   [leihs.inventory.client.routes.components.header :as header]
   [uix.core :as uix :refer [defui $]]
   [uix.dom]))

(defui layout []
  (let [{:keys [profile]} (router/useLoaderData)]

   ($ TooltipProvider
      ($ ErrorBoundary
         ($ :<>
            ($ header/main profile)
            ($ :main {:className "container"}
               ($ Outlet)
               ($ Toaster {:position "top-center"
                           :closeButton true
                           :richColors true})))))))
