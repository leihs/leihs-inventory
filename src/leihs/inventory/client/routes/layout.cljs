(ns leihs.inventory.client.routes.layout
  (:require
   ["react-router-dom" :as router]
   ;; [client.routes.components.aside :as aside]
   [leihs.inventory.client.routes.components.header :as header]
   [uix.core :as uix :refer [defui $]]
   [uix.dom]))

(defui layout []
  ($ :<>
     ($ header/main)
     ($ :main {:className "container"}
        ($ router/Outlet))))
