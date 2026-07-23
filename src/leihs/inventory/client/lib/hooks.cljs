(ns leihs.inventory.client.lib.hooks
  (:require ["react-hook-form" :refer [useWatch]]
            ["react-router" :as router]
            [leihs.core.core :refer [detect]]
            [leihs.inventory.client.lib.utils :refer [jc cj]]
            [uix.core :as uix]))

;; NOTE: would be nicer to user defhook macro, but somehow the linting is broken ( maybe beacause of uix version? ) 

(defn use-current-pool []
  (let [[current-pool set-current-pool!] (uix/use-state nil)
        {:keys [pool-id]} (jc (router/useParams))
        profile (router/useRouteLoaderData "root")
        available_inventory_pools (-> profile
                                      :profile
                                      :available_inventory_pools)]

    (uix/use-effect
     (fn []
       (let [pool-id (->> available_inventory_pools (detect #(= pool-id (:id %))))]
         (when pool-id
           (set-current-pool! pool-id))))
     [pool-id available_inventory_pools])

    current-pool))

;; NOTE: docs https://usehooks.com/usedebounce
;; Returns [debounced-value reset!] where reset! cancels any pending timer
;; and immediately sets the debounced value to the given argument.
(defn use-debounce [value delay]
  (let [[debounced-value set-debounced-value!] (uix/use-state value)
        timer-ref (uix/use-ref nil)
        clear! (uix/use-callback
                (fn [v]
                  (when-let [t @timer-ref]
                    (js/clearTimeout t)
                    (reset! timer-ref nil))
                  (set-debounced-value! v))
                [])]

    (uix/use-effect
     (fn []
       (let [handler (js/setTimeout
                      (fn [] (set-debounced-value! value))
                      delay)]
         (reset! timer-ref handler)
         (fn [] (js/clearTimeout handler))))
     [value delay clear!])

    [debounced-value clear!]))

;; NOTE: docs https://usehooks.com/useWindowScroll
(defn use-window-scroll []
  (let [[state set-state!] (uix/use-state {:x nil :y nil})

        scroll-to (uix/use-callback
                   (fn [& args]
                     (cond
                       (object? (first args))
                       (.scrollTo js/window (first args))

                       (and (number? (first args)) (number? (second args)))
                       (.scrollTo js/window (first args) (second args))

                       :else
                       (throw (js/Error. "Invalid arguments passed to scrollTo. See here for more info. https://developer.mozilla.org/en-US/docs/Web/API/Window/scrollTo"))))
                   [])]

    (uix/use-layout-effect
     (fn []
       (let [handle-scroll (fn []
                             (set-state! {:x (.-scrollX js/window)
                                          :y (.-scrollY js/window)}))]
         (handle-scroll)
         (.addEventListener js/window "scroll" handle-scroll)

         (fn []
           (.removeEventListener js/window "scroll" handle-scroll))))
     [])

    [state scroll-to]))

;; NOTE: docs https://usehooks.com/useWindowSize
(defn use-window-size []
  (let [[size set-size!] (uix/use-state {:width nil :height nil})]

    (uix/use-layout-effect
     (fn []
       (let [handle-resize (fn []
                             (set-size! {:width (.-innerWidth js/window)
                                         :height (.-innerHeight js/window)}))]
         (handle-resize)
         (.addEventListener js/window "resize" handle-resize)

         (fn []
           (.removeEventListener js/window "resize" handle-resize))))
     [])

    size))

;; NOTE: docs https://www.30secondsofcode.org/react/s/use-mutation-observer/
(defn use-mutation-observer
  [{:keys [ref callback options]
    :or {options {:attributes false
                  :subtree false
                  :childList false
                  :characterData false}}}]

  (uix/use-effect
   (fn []
     (when-let [el (.-current ref)]
       (let [observer (js/MutationObserver. callback)]
         (.observe observer el (clj->js options))
         (fn []
           (.disconnect observer)))))
   [callback options ref]))

;; NOTE: docs https://usehooks.com/useNetworkStatus
(defn- get-connection []
  (or (.-connection js/navigator)
      (.-mozConnection js/navigator)
      (.-webkitConnection js/navigator)))

(defn- shallow-equal? [obj1 obj2]
  (let [keys1 (js/Object.keys obj1)
        keys2 (js/Object.keys obj2)]
    (and (= (.-length keys1) (.-length keys2))
         (every? (fn [key]
                   (= (aget obj1 key) (aget obj2 key)))
                 keys1))))

(defn- use-network-state-subscribe [callback]
  (.addEventListener js/window "online" callback #js {:passive true})
  (.addEventListener js/window "offline" callback #js {:passive true})

  (when-let [connection (get-connection)]
    (.addEventListener connection "change" callback #js {:passive true}))

  (fn []
    (.removeEventListener js/window "online" callback)
    (.removeEventListener js/window "offline" callback)

    (when-let [connection (get-connection)]
      (.removeEventListener connection "change" callback))))

(defn- get-network-state-server-snapshot []
  (throw (js/Error. "useNetworkState is a client-only hook")))

(defn use-network-state []
  (let [cache (uix/use-ref {})
        get-snapshot (uix/use-callback
                      (fn []
                        (let [online (.-onLine js/navigator)
                              connection (get-connection)
                              next-state #js {:online online
                                              :downlink (when connection (.-downlink connection))
                                              :downlinkMax (when connection (.-downlinkMax connection))
                                              :effectiveType (when connection (.-effectiveType connection))
                                              :rtt (when connection (.-rtt connection))
                                              :saveData (when connection (.-saveData connection))
                                              :type (when connection (.-type connection))}]
                          (if (shallow-equal? (.-current cache) next-state)
                            (.-current cache)
                            (do
                              (set! (.-current cache) next-state)
                              next-state))))
                      [])]

    (uix/use-sync-external-store
     use-network-state-subscribe
     get-snapshot
     get-network-state-server-snapshot)))

;; NOTE: docs https://usehooks.com/usemediaquery
(defn use-media-query [query]
  (let [subscribe (uix/use-callback
                   (fn [callback]
                     (let [match-media (.matchMedia js/window query)]
                       (.addEventListener match-media "change" callback)
                       (fn []
                         (.removeEventListener match-media "change" callback))))
                   [query])

        get-snapshot (fn []
                       (.-matches (.matchMedia js/window query)))

        get-server-snapshot (fn []
                              (throw (js/Error. "useMediaQuery is a client-only hook")))]

    (uix/use-sync-external-store
     subscribe
     get-snapshot
     get-server-snapshot)))

;; ---------------------------------------------------------------------------
;; use-barcode-scanner
;; ---------------------------------------------------------------------------
;; Detects barcode scanner input from global keydown events.
;; Barcode scanners type characters very quickly (< 50ms between keystrokes)
;; followed by an Enter key. Returns [scanned-code clear-scan!] where
;; scanned-code is the last completed scan and clear-scan! resets it to nil
;; so the same code can be scanned again.

(defn use-barcode-scanner []
  (let [[scanned-code set-scanned-code!] (uix/use-state nil)
        clear! (fn []
                 (set-scanned-code! nil))]
    (uix/use-effect
     (fn []
       (let [buffer (atom "")
             last-time (atom 0)
             timer (atom nil)
             threshold-ms 50

             commit!
             (fn []
               (let [code @buffer]
                 (reset! buffer "")
                 (when (>= (count code) 3)
                   (set-scanned-code! code))))

             on-keydown
             (fn [e]
               (let [now (js/Date.now)
                     elapsed (- now @last-time)
                     key (.-key e)
                     active (.-activeElement js/document)
                     is-barcode? (.. active -dataset -barcode)
                     is-field? (contains? #{"INPUT" "TEXTAREA" "SELECT"} (.-tagName active))
                     has-modifier? (or (.-metaKey e) (.-ctrlKey e) (.-altKey e))]

                 (when-not has-modifier?
                   (reset! last-time now))

                 (when (and (not has-modifier?)
                            (or (not is-field?) (is-barcode?))
                            (< elapsed threshold-ms))

                   (cond
                     (= key "Enter")
                     (do
                       (.preventDefault e)
                       (commit!))

                     (= (count key) 1)
                     (do
                       (.preventDefault e)
                       (when @timer (js/clearTimeout @timer))
                       (swap! buffer str key)
                       (reset! timer (js/setTimeout commit! 200)))))))]

         (.addEventListener js/document "keydown" on-keydown)
         (fn []
           (.removeEventListener js/document "keydown" on-keydown)
           (when @timer (js/clearTimeout @timer)))))
     [])
    [scanned-code clear!]))

;; ---------------------------------------------------------------------------
;; use-dependent-fields
;; ---------------------------------------------------------------------------
;; Manages auto-insertion/removal of dependent fields in a useFieldArray:
;;   - retired=true  → insert retired_reason after it
;;   - building_id selected → insert room_id after it
;;   - building_id changed  → update room_id with new rooms URL
;;
;; Usage:
;;   (use-dependent-fields {:field-array field-array
;;                          :control     control
;;                          :pool-id     pool-id
;;                          :name        field-array-name
;;                          :extra-field-props extra})
;;
;; `extra-field-props` is an optional map merged into every generated field
;; entry (e.g. {:allowed-operators ["$eq"] :operator "$eq"} for the filter
;; variant).

(defn use-dependent-fields
  [{:keys [field-array control pool-id name extra-field-props]}]
  (let [{:keys [fields insert remove update]} (jc field-array)
        watched-values (jc (useWatch (cj {:control control :name name})))

        has-reason-ref (uix/use-ref (->> fields
                                         (some #(= (:name %) "retired_reason"))
                                         boolean))
        has-room-ref (uix/use-ref (->> fields
                                       (some #(= (:name %) "room_id"))
                                       boolean))
        building-value-ref (uix/use-ref nil)
        extra-ref (uix/use-ref extra-field-props)]

    (uix/use-effect
     (fn []
       (let [make-field (fn [m] (cj (merge {:id (str (random-uuid))}
                                           @extra-ref
                                           m)))
             ;; Look up values by name, not by index, to avoid stale reads
             ;; when indices shift after an unrelated field is removed
             find-watched-value (fn [field-name]
                                  (->> watched-values
                                       (filter #(= (:name %) field-name))
                                       first
                                       :value))

             retired-index (->> fields
                                (map-indexed vector)
                                (filter #(= (:name (second %)) "retired"))
                                ffirst)
             retired? (= (find-watched-value "retired") "true")

             reason-index (->> fields
                               (map-indexed vector)
                               (filter #(= (:name (second %)) "retired_reason"))
                               ffirst)

             building-index (->> fields
                                 (map-indexed vector)
                                 (filter #(= (:name (second %)) "building_id"))
                                 ffirst)
             building-value (find-watched-value "building_id")

             room-index (->> fields
                             (map-indexed vector)
                             (filter #(= (:name (second %)) "room_id"))
                             ffirst)
             room-value (find-watched-value "room_id")

             building-changed? (and (not= @building-value-ref :unset)
                                    (not= (:value building-value)
                                          (:value @building-value-ref)))]

         (cond
           (and retired?
                (nil? reason-index)
                (not @has-reason-ref))
           (do
             (reset! has-reason-ref true)
             (insert (inc retired-index)
                     (make-field {:name "retired_reason"
                                  :operator "$ilike"
                                  :component "textarea"
                                  :value ""})))

           (and (not retired?)
                (some? reason-index)
                @has-reason-ref)
           (do
             (reset! has-reason-ref false)
             (js/setTimeout #(remove reason-index) 0))

           (and (not building-value)
                (some? room-index)
                @has-room-ref)
           (do
             (reset! has-room-ref false)
             (reset! building-value-ref nil)
             (js/setTimeout #(remove room-index) 0))

           ;; Building value changed and room already exists → update room URL/value
           (and building-changed?
                @has-room-ref
                (some? room-index)
                (:value building-value))
           (do
             (reset! building-value-ref building-value)
             (update room-index
                     (make-field {:name "room_id"
                                  :component "autocomplete"
                                  :operator "$eq"
                                  :value (if (nil? (:label room-value)) room-value nil)
                                  :props {:values-url (str "/inventory/" pool-id
                                                           "/rooms/?building_id="
                                                           (:value building-value))}})))

           ;; Building selected for first time and room not yet in list → insert room
           (and (contains? building-value :value)
                (nil? room-index)
                (not @has-room-ref))
           (do
             (reset! building-value-ref building-value)
             (reset! has-room-ref true)
             (insert (inc building-index)
                     (make-field {:name "room_id"
                                  :component "autocomplete"
                                  :operator "$eq"
                                  :value nil
                                  :props {:values-url (str "/inventory/" pool-id
                                                           "/rooms/?building_id="
                                                           (:value building-value))}}))))))

     [fields watched-values insert remove update pool-id])

    ;; Return values so callers can use them
    {:fields fields
     :has-reason-ref has-reason-ref
     :has-room-ref has-room-ref}))
