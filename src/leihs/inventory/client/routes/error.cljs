(ns leihs.inventory.client.routes.error
  (:require
   ["@@/alert-dialog" :refer [AlertDialog AlertDialogAction AlertDialogCancel
                              AlertDialogContent AlertDialogDescription
                              AlertDialogFooter AlertDialogHeader
                              AlertDialogTitle AlertDialogTrigger]]
   ["@@/button" :refer [Button]]
   ["lucide-react" :refer [TriangleAlert SearchAlert CloudLightning Bomb Ban]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router :refer [useRouteError useNavigate Link]]
   [uix.core :as uix :refer [$ defui]]))

;; Determine error type
;; - HTTP errors (from React Router): check for status property -> not-found, 
;;                                                                 unauthorized, 
;;                                                                 forbidden, 
;;                                                                 bad-request, 
;;                                                                 server-error
;; - Loader/Action errors: check for cause property -> loader-error / client-error
;; - Exceptions: check for message property -> application-error
;; - Unknown: default case -> unknown
;;
;; Return keywords representing error types
;; :not-found, :unauthorized, :forbidden, :bad-request, :server-error
;; :loader-error, :client-error, 
;; :application-error, 
;; :unknown
;;
;; Used to customize error display and actions

(defn get-error-type
  "Determines the type of error based on its properties"
  [error]
  (if (nil? error)
    :unknown
    (cond
      ;; HTTP errors (from React Router)
      (.-status error)
      (let [status (.-status error)]
        (cond
          (= status 401) :unauthorized
          (= status 403) :forbidden
          (= status 404) :not-found
          (and (< status 500)
               (>= status 400)) :bad-request
          (>= status 500) :server-error
          :else :unknown))

      ;; Loader/Action errors
      (exists? (.-cause error))
      (case (.-cause error)
        "loader-error" :loader-error
        "action-error" :client-error
        nil)

      ;; Exceptions
      (.-message error) :application-error

      ;; Unknown
      :else :unknown)))

(defui ErrorIcon [{:keys [type]}]
  (case type
    :not-found ($ SearchAlert {:class-name "text-blue-600"})
    :unauthorized ($ Ban {:class-name "text-yellow-600"})
    :forbidden ($ Ban {:class-name "text-red-600"})
    :bad-request ($ TriangleAlert {:class-name "text-yellow-600"})
    :server-error ($ CloudLightning {:class-name "text-red-600"})

    :loader-error ($ CloudLightning {:class-name "text-yellow-600"})
    :client-error ($ TriangleAlert {:class-name "text-yellow-600"})

    :application-error ($ TriangleAlert {:class-name "text-red-600"})
    ($ Bomb {:class-name "text-red-600"})))

(defui ErrorTitle [{:keys [type]}]
  (let [[t] (useTranslation)]
    (case type
      :not-found (t "error.boundary.not_found.title")
      :unauthorized (t "error.boundary.unauthorized.title")
      :forbidden (t "error.boundary.forbidden.title")
      :bad-request (t "error.boundary.bad_request.title")
      :server-error (t "error.boundary.server_error.title")

      :loader-error (t "error.boundary.loader_error.title")
      :client-error (t "error.boundary.client_error.title")

      :application-error (t "error.boundary.application_error.title")
      (t "error.boundary.unknown.title"))))

(defui ErrorDescription [{:keys [type error]}]
  (let [[t] (useTranslation)
        description (case type
                      :not-found (t "error.boundary.not_found.description")
                      :unauthorized (t "error.boundary.unauthorized.description")
                      :forbidden (t "error.boundary.forbidden.description")
                      :bad-request (t "error.boundary.bad_request.description")
                      :server-error (t "error.boundary.server_error.description")

                      :loader-error (t "error.boundary.loader_error.description")
                      :client-error (if (.-statusText error)
                                      (str (.-statusText error) ": " (.-data error))
                                      (t "error.boundary.client_error.description"))

                      :application-error (t "error.boundary.application_error.description")
                      (t "error.boundary.unknown.description"))]

    ($ :p {:class-name "text-gray-600 mb-6"}
       description)))

(defui ErrorDetails [{:keys [error show]}]
  (when show
    ($ :details {:class-name "mt-8"}
       ($ :summary {:class-name "cursor-pointer text-sm font-medium text-gray-700 mb-2"}
          "🔧 Technical Details")
       ($ :div {:class-name "bg-gray-50 p-4 rounded-md border border-gray-200 overflow-auto"}
          ($ :code {:class-name "text-xs text-gray-800 break-all whitespace-pre-wrap"}
             (if (.-stack error)
               (.-stack error)
               (js/JSON.stringify error nil 2)))))))

(defui page []
  (let [[t] (useTranslation)
        error (useRouteError)
        navigate (useNavigate)
        error-type (get-error-type error)
        show-details? (or (= error-type :application-error)
                          (= error-type :unknown)
                          (not (.-status error)))]

    ($ AlertDialog {:default-open true}

       ($ AlertDialogTrigger {:as-child true}
          ($ Button {:variant "destructive"
                     :class-name "fixed bottom-4 right-4 z-10"}
             (t "error.boundary.open")))

       ($ AlertDialogContent
          ($ AlertDialogHeader
             ($ AlertDialogTitle {:as-child true}
                ($ :h2 {:class-name "flex items-center"}
                   ($ :span {:class-name "mr-2"}
                      ($ ErrorIcon {:type error-type}))
                   ($ ErrorTitle {:type error-type}))))

          ($ AlertDialogDescription {:as-child true}
             ($ :div
                ($ ErrorDescription {:type error-type
                                     :error error})

                ;; HTTP Status (if available)
                (when (.-status error)
                  ($ :p {:class-name "text-sm text-gray-500 mb-6"}
                     (str "Status: " (.-status error))))

                ($ ErrorDetails {:error error
                                 :show show-details?})))

          ($ AlertDialogFooter

            ;; Action buttons
            ;; Go Back button (for non-404 errors)
             #_(when (not= error-type :not-found)
                 ($ AlertDialogCancel {:asChild true}
                    ($ Link {:to "../"}
                       (t "error.boundary.actions.go_back"))))

             ;; Retry button (for network/loader errors)
             (when (or (= error-type :loader-error)
                       (= error-type :client-error))
               ($ AlertDialogCancel {:on-click #(js/window.location.reload)}
                  (t "error.boundary.actions.retry")))

             ($ AlertDialogCancel
                (t "error.boundary.actions.close"))

             ;; Go Home button (always available)
             (case error-type
               :forbidden
               ($ AlertDialogAction {:asChild true}
                  ($ :a {:href "/sign-in"}
                     (t "error.boundary.actions.go_signin")))

               :unauthorized
               ($ AlertDialogAction {:asChild true}
                  ($ :a {:href "/sign-in"}
                     (t "error.boundary.actions.go_signin")))

               ($ AlertDialogAction {:asChild true}
                  ($ Link {:to "/inventory/"}
                     (t "error.boundary.actions.go_home"))))))

;; Technical details (collapsible)
       )))
