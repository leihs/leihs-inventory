(ns leihs.inventory.server.run
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.string]
   [clojure.tools.cli :as cli]
   [leihs.core.db :as db]
   [leihs.core.http-server :as http-server]
   [leihs.core.shutdown :as shutdown]
   [leihs.core.status :as status]
   [leihs.core.url.jdbc]
   [leihs.inventory.server.app-handler :as app-handler]
   [leihs.inventory.server.constants :refer [MAX_REQUEST_BODY_SIZE_MB]]
   [logbug.catcher :as catcher]
   [reitit.coercion.schema]
   [taoensso.timbre :refer [info]]))

(defn run [options]
  (catcher/snatch
   {:return-fn (fn [_] (System/exit -1))}
   (info "Invoking run with options: " options)
   (shutdown/init options)
   (let [status (status/init)
         options (assoc options :http-max-body (* MAX_REQUEST_BODY_SIZE_MB 1024 1024))]
     (db/init options (:health-check-registry status))
     (http-server/start options (app-handler/init)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def cli-options
  (concat
   [["-h" "--help"]
    shutdown/pid-file-option]
   (http-server/cli-options :default-http-port 3260)
   db/cli-options))

(defn main-usage [options-summary & more]
  (->> ["Leihs Inventory"
        ""
        "usage: leihs-inventory [<gopts>] run [<opts>] [<args>]"
        ""
        "Options:"
        options-summary
        ""
        ""
        (when more
          ["-------------------------------------------------------------------"
           (with-out-str (pprint more))
           "-------------------------------------------------------------------"])]
       flatten (clojure.string/join \newline)))

(defn main [gopts args]
  (let [{:keys [options summary]} (cli/parse-opts args cli-options :in-order true)
        options (merge gopts options)]
    (cond
      (:help options) (info (main-usage summary {:args args :options options}))
      :else (run options))))
