(ns leihs.inventory.client.routes.debug.components.error
  (:require
   ["@@/button" :refer [Button]]
   ["@@/item" :refer [Item ItemTitle ItemContent]]
   [uix.core :as uix :refer [defui $ use-state]]))

(defui error-trigger []
  "Component that throws an error during render when triggered"
  (throw (js/Error. "Test error thrown during render!")))

(defui error-component []
  (let [[should-error set-should-error] (use-state false)]
    ($ :div
       ;; Conditionally render the error-throwing component
       (when should-error
         ($ error-trigger))

       ($ Button {:variant "destructive"
                  :on-click #(set-should-error true)}
          "Trigger Render Error"))))

(defui main []
  ($ Item {:variant "outline"
           :className "flex flex-col max-w-fit"}

     ($ ItemContent
        ($ ItemTitle
           "Error Boundary Tests")
        ($ :div
           ($ :p.text-sm.text-gray-600.mb-4
              "Test the React Router error boundary with different types of errors:")

           ($ :div.space-y-4
             ;; Render Error Test
              ($ :div
                 ($ :h3.font-semibold "1. Render Error Test")
                 ($ :p.text-sm.text-gray-600.mb-2
                    "This triggers an error during component rendering:")
                 ($ error-component))

              ;; Loader Error Test
              ($ :div
                 ($ :h3.font-semibold.mt-4 "2. Loader Error Test")
                 ($ :p.text-sm.text-gray-600.mb-2
                    "This tests loader errors (navigate to a route with a broken loader):")

                 ($ Button {:asChild true
                            :variant "destructive"}
                    ($ :a
                       {:href "/inventory/test-error"}
                       "Test Loader Error")))

              ;; 404 Error Test
              ($ :div
                 ($ :h3.font-semibold.mt-4 "3. 404 Error Test")
                 ($ :p.text-sm.text-gray-600.mb-2
                    "Test the 404 not found error page:")
                 ($ Button {:asChild true
                            :variant "destructive"}
                    ($ :a
                       {:href "/inventory/foo/bar/fizz/buzz"}
                       "Test 404 Error"))))))))
