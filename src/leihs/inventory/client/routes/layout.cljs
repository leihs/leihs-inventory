(ns leihs.inventory.client.routes.layout
  (:require
   ["@@/sonner" :refer [Toaster]]
   ["react-router-dom" :as router :refer [Outlet]]
   [leihs.inventory.client.routes.components.header :as header]
   [uix.core :as uix :refer [defui $]]
   [uix.dom]))

(defui layout []
  ($ :<>
     ($ header/main)
     ($ :main {:className "container"}
        ($ Outlet)
        ($ Toaster {:position "top-center"
                    :richColors true}))))
