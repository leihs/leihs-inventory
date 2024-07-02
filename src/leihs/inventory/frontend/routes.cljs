(ns leihs.inventory.frontend.routes
  (:require
   [leihs.inventory.frontend.pages.debug.index :as debug-index]
   [leihs.inventory.frontend.pages.home.home :as home]
   [leihs.inventory.frontend.pages.models.index :as models-index]))

(def handler-map
  {:home #'home/page
   :models-index #'models-index/page
   :debug-index #'debug-index/page})
