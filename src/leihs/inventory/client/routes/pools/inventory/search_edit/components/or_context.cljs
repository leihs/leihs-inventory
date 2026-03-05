(ns leihs.inventory.client.routes.pools.inventory.search-edit.components.or-context
  (:require
   [uix.core :as uix :refer [$ defui]]))

;; Create the context with default value
(def or-context (uix/create-context nil))

;; Provider component
(defui OrProvider [{:keys [index form blocks children]}]
  ($ (.-Provider or-context)
     {:value {:index index
              :form form
              :blocks blocks}}
     children))

;; Hook to consume the context
(defn use-or []
  (let [ctx (uix/use-context or-context)]
    (when (nil? ctx)
      (throw (js/Error. "use-or must be used within OrProvider")))
    ctx))
