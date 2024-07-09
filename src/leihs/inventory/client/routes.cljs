(ns leihs.inventory.client.routes
  (:require
   [leihs.inventory.client.pages.debug.index :as debug-index]
   [leihs.inventory.client.pages.home.home :as home]
   [leihs.inventory.client.pages.models.index :as models-index]))

(def handler-map
  {:home #'home/page
   :models-index #'models-index/page
   :debug-index #'debug-index/page})
