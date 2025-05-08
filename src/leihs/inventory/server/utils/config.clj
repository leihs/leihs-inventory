; Copyright © 2013 - 2017 Dr. Thomas Schank <Thomas.Schank@AlgoCon.ch>
; Licensed under the terms of the GNU Affero General Public License v3.
; See the "LICENSE.txt" file provided with this software.

(ns leihs.inventory.server.utils.config
  (:require
   [clj-yaml.core :as yaml]
   [clojure.java.io :as io]
   [clojure.set :refer [difference]]
   [leihs.inventory.server.utils.core :refer [deep-merge]]
   [leihs.inventory.server.utils.fs :refer :all]
   [logbug.catcher :refer [snatch]]
   [taoensso.timbre :refer [info warn]]))

(defonce ^:private conf (atom {}))

(defn get-config [] @conf)

(defonce default-opts {:defaults {}
                       :overrides {}
                       :resource-names ["config_default.yml"]
                       :filenames [(system-path "." "config" "config.yml")
                                   (system-path ".." "config" "config.yml")]})

(defonce opts (atom {}))

(defn get-opts [] @opts)

;##############################################################################

(defn exit! []
  (System/exit -1))

;##############################################################################

(defn merge-into-conf [params]
  (when-not (= (get-config)
               (deep-merge (get-config) params))
    (let [new-config (swap! conf
                            (fn [current-config params]
                              (deep-merge current-config params))
                            params)]
      (info "config changed to " new-config))))

(defn slurp-and-merge [config slurpable]
  (->> (slurp slurpable)
       yaml/parse-string
       (deep-merge config)))

(defn read-and-merge-resource-name-configs [config]
  (reduce (fn [config resource-name]
            (if-let [io-resource (io/resource resource-name)]
              (snatch {} (slurp-and-merge config io-resource))
              config))
          config (:resource-names @opts)))

(defn read-and-merge-filename-configs [config]
  (reduce (fn [config filename]
            (if (.exists (io/as-file filename))
              (snatch {} (slurp-and-merge config filename))
              config))
          config (:filenames @opts)))

(defn read-configs-and-merge-into-conf []
  (-> (:defaults @opts)
      (deep-merge (get-config))
      read-and-merge-resource-name-configs
      read-and-merge-filename-configs
      (deep-merge (:overrides @opts))
      merge-into-conf))

;### Initialize ###############################################################

(defn initialize [options]
  (snatch {:throwable Throwable
           :level :fatal
           :return-fn (fn [_] (exit!))}
          (let [default-opt-keys (-> default-opts keys set)]
            (assert
             (empty?
              (difference (-> options keys set)
                          default-opt-keys))
             (str "Opts must only contain the following keys: " default-opt-keys))
            (Thread/sleep 1000)
            (reset! conf {})
            (let [new-opts (deep-merge default-opts options)]
              (reset! opts new-opts)
              (read-configs-and-merge-into-conf)))))
