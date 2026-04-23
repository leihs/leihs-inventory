(ns leihs.inventory.client.routes.debug.components.error
  (:require
   ["@@/button" :refer [Button]]
   ["@@/item" :refer [Item ItemTitle ItemContent]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router]
   ["sonner" :refer [toast]]
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

(defui action-error-component []
  (let [fetcher (router/useFetcher)
        last-data (uix/use-ref nil)
        [t] (useTranslation)]

    (uix/use-effect
     (fn []
       (let [data (.-data fetcher)
             state (.-state fetcher)]
         (when (and (= state "idle")
                    (some? data)
                    (not= data @last-data))
           (reset! last-data data)
           (when (= (aget data "status") "error")
             (.. toast (error (t "error.action.error")
                              (clj->js {:description (t "error.action.error_detail"
                                                        #js {:httpStatus (aget data "httpStatus")})})))))))
     [fetcher t])

    ($ :div.flex.gap-2
       ($ fetcher.Form {:method "POST"}
          ($ :input {:type "hidden" :name "error-type" :value "not-found"})
          ($ Button {:type "submit" :variant "destructive"}
             "Test Action Error (404)"))
       ($ fetcher.Form {:method "POST"}
          ($ :input {:type "hidden" :name "error-type" :value "bad-request"})
          ($ Button {:type "submit" :variant "destructive"}
             "Test Action Error (400)")))))

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
                       "Test 404 Error")))

              ;; Action Error Tests
              ($ :div
                 ($ :h3.font-semibold.mt-4 "4. Action Error Tests")
                 ($ :p.text-sm.text-gray-600.mb-2
                    "Test action errors (shows a toast without crashing the page):")
                 ($ action-error-component)))))))
