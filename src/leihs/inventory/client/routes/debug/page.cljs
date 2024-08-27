(ns leihs.inventory.client.routes.debug.page
  (:require
   [uix.core :as uix :refer [defui $]]
   [uix.dom]))

(defui page []
  ($ :h2 "Some routing tests"))
