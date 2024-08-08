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
   [logbug.catcher :as catcher]
   [reitit.coercion.schema]
   [taoensso.timbre :refer [info]]))

(defn run [options]
  (println ">o> server.run.run")
  (catcher/snatch
    {:return-fn (fn [e] (System/exit -1))}
    (info "Invoking run with options: " options)
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
  (->> ["leihs-inventory2"
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
  (println ">o> server.run.main")
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-options :in-order true)
        pass-on-args (->> [options (rest arguments)]
                       flatten (into []))
        p (println ">o> (not used) pass-on-args=" pass-on-args)
        options (merge gopts options)

        p (println ">o> options=" options)
        ]
    (cond
      (:help options) (println (main-usage summary {:args args :options options}))
      :else (run options))))
