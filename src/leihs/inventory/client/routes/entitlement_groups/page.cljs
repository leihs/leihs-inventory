(ns leihs.inventory.client.routes.entitlement-groups.page
  (:require
   [leihs.inventory.client.lib.utils :refer [jc]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defui page []
  ($ :div
     ($ :h1 {:class-name "text-2xl"} "hello entitlement-groups")))
