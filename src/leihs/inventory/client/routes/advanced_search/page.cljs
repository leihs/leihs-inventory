(ns leihs.inventory.client.routes.advanced-search.page
  (:require
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defui page []
  ($ :div
     ($ :h1 {:class-name "text-2xl"} "hello advanced search")))
