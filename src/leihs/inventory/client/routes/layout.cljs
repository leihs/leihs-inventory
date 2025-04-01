(ns leihs.inventory.client.routes.layout
  (:require
   ["@@/sonner" :refer [Toaster]]
   ["react-router-dom" :as router :refer [Outlet]]
   [leihs.inventory.client.routes.components.header :as header]
   [uix.core :as uix :refer [defui $]]
   [uix.dom]))

(defui layout []
  (let [profile (router/useLoaderData)]
    ($ :<>
       ($ header/main profile)
       ($ :main {:className "container"}
          ($ Outlet)
          ($ Toaster {:position "top-center"
                      :richColors true})))))
