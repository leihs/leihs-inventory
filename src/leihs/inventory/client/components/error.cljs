(ns leihs.inventory.client.components.error
  (:require [uix.core :as uix :refer [defui $ use-state]]))

(defui error-trigger []
  "Component that throws an error during render when triggered"
  (throw (js/Error. "Test error thrown during render!")))

(defui error-component []
  (let [[should-error set-should-error] (use-state false)]
    ($ :div.p-4
       ($ :h2.text-xl.font-bold.mb-4 "Error Boundary Test Component")
       ($ :p.text-sm.text-gray-600.mb-4 
          "This button will trigger a render error that should be caught by the React Router error boundary.")
       
       ;; Conditionally render the error-throwing component
       (when should-error
         ($ error-trigger))
       
       ($ :button.bg-red-500.hover:bg-red-700.text-white.font-bold.py-2.px-4.rounded
          {:on-click #(set-should-error true)}
          "Trigger Render Error"))))
