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
   [leihs.inventory.server.swagger-api :as sui]
   [leihs.inventory.server.utils.config :refer [initialize get-config]]
   [logbug.catcher :as catcher]
   [reitit.coercion.schema]
   [taoensso.timbre :refer [info]]))

(defn run [options]
  (catcher/snatch
   {:return-fn (fn [e] (System/exit -1))}
   (info 'leihs.inventory.server.run "initializing ...")
   (let [db-name (System/getenv "RAILS_ENV")
         filenames (cond-> ["./config/settings.yml"
                            "../config/settings.yml",
                            "./config/settings.local.yml"
                            "../config/settings.local.yml"]
                     (= db-name "test") (conj "./config/test-settings.yml"))]
     (initialize {:filenames filenames}))
   (info "Effective startup options " options)
   (info "Effective startup config " (get-config))
   (shutdown/init options)
   (let [status (status/init)]
     (db/init options (:health-check-registry status)))
   (http-server/start options (sui/create-app options))))

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
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-options :in-order true)
        pass-on-args (->> [options (rest arguments)]
                          flatten (into []))
        options (merge gopts options)]
    (cond
      (:help options) (println (main-usage summary {:args args :options options}))
      :else (run options))))
