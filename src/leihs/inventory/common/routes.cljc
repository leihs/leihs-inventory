(ns leihs.inventory.common.routes
  )

(defn wrap-html-only [handler]
  (fn [request]
    (let [accept-header (get-in request [:headers "accept"])]
      (if (and accept-header (re-matches #"^.*text/html.*$" accept-header))
        (handler request)
        ;(response/not-acceptable "Not Acceptable: text/html required")

        {:status 404 :body "Not Acceptable: text/html required"}
        ))))

(def routes
  [["/inventory"
    ["" {:name :home}]

    ["/models" {:name :models-index}]
    ["/debug" {:name :debug-index}]

    ]])
