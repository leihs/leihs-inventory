(ns leihs.inventory.client.lib.client
  (:require ["axios" :as axios]
            ["axios-cache-interceptor" :as axios-cache]
            [leihs.inventory.client.lib.csrf :as csrf]
            [leihs.inventory.client.lib.utils :refer [cj]]
            ["sonner" :refer [toast]]))

(def instance
  (.. axios -default (create
                      #js {:baseURL ""
                           :headers #js {"Content-Type" "application/json"
                                         "Accept" "application/json"}
                           :xsrfCookieName csrf/cookie-name
                           :xsrfHeaderName csrf/header-field-name})))

(def http-client (axios-cache/setupCache instance))


;; Dev diagnostics for failed HTTP responses
(let [env (.. js/process -env -NODE_ENV)
      is-prod (= env "production")]
  (.. http-client -interceptors -response
      (use
       (fn [res] res)
       (fn [error]
         (let [resp (.-response error)
               cfg (.-config error)
               status (when resp (.-status resp))
               status-text (when resp (.-statusText resp))
               url (when cfg (.-url cfg))
               method (when cfg (.-method cfg))]
           (when (not is-prod)
             (.. toast (error (str "HTTP " (or status "Error")
                                   (when status-text (str " " status-text)))
                              (cj {:description (str (or method "GET") " " (or url ""))}))))
           (js/Promise.reject error))))))
