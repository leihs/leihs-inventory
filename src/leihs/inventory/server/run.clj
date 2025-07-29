(ns leihs.inventory.server.run
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.string]
   [clojure.tools.cli :as cli]
   [leihs.core.db :as db]
   [leihs.core.http-cache-buster2 :as cache-buster2]
   [leihs.core.http-server :as http-server]
   [leihs.core.shutdown :as shutdown]
   [leihs.core.status :as status]
   [leihs.core.url.jdbc]
   [leihs.inventory.server.constants :refer [MAX_REQUEST_BODY_SIZE_MB]]
   [leihs.inventory.server.swagger-api :as sui]
   [logbug.catcher :as catcher]
   [reitit.coercion.schema]
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.default-charset :refer [wrap-default-charset]]
   [taoensso.timbre :refer [info]]))

(def cache-bust-options
  {:cache-bust-paths [#"^/inventory/assets/.*\.(js|css|png|jpg|svg|woff2?)$"]
   :never-expire-paths []
   :cache-enabled? true})

(defn app [options]
  (-> (sui/create-app options)
      (cache-buster2/wrap-resource "public" cache-bust-options)
      (wrap-content-type {:mime-types {"svg" "image/svg+xml"}})
      (wrap-default-charset "utf-8")))

(defn run [options]
  (catcher/snatch
   {:return-fn (fn [e] (System/exit -1))}
   (info "Invoking run with options: " options)
   (shutdown/init options)
   (let [status (status/init)
         options (assoc options :http-max-body (* MAX_REQUEST_BODY_SIZE_MB 1024 1024))]
     (db/init options (:health-check-registry status))
     (http-server/start options (app options)))))

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
      (:help options) (info (main-usage summary {:args args :options options}))
      :else (run options))))
