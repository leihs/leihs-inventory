(ns leihs.inventory.client.routes.components.theme-provider
  (:require
   ["react" :as react]
   [uix.core :as uix :refer [$ defui]]))

;; Theme type: "dark" | "light" | "system"
;; copy pasted from here: https://ui.shadcn.com/docs/dark-mode/vite
;; rewritten by claude with minor changes
;; could be written more nicely, but needs uix update ( defcontext, uix context api )

(def initial-state
  [:theme "system"
   :set-theme (fn [_] nil)])

(def theme-provider-context
  (react/createContext (clj->js initial-state)))

(defui ThemeProvider
  [{:keys [children default-theme storage-key]
    :or {default-theme "system"
         storage-key "leihs-inventory-theme"}}]
  (let [[theme set-theme!] (uix/use-state
                            (fn []
                              (or (.getItem js/localStorage storage-key)
                                  default-theme)))

        ;; Context value with theme and setter
        value (uix/use-memo
               (fn []
                 #js {:theme theme
                      :setTheme (fn [new-theme]
                                  (.setItem js/localStorage storage-key new-theme)
                                  (set-theme! new-theme))})
               [theme storage-key])]

    ;; Effect to apply theme class to document root
    (uix/use-effect
     (fn []
       (let [root (.-documentElement js/document)]
               ;; Remove existing theme classes
         (-> root .-classList (.remove "light" "dark"))

               ;; Apply new theme
         (if (= theme "system")
           (let [system-theme (if (.-matches (.matchMedia js/window "(prefers-color-scheme: dark)"))
                                "dark"
                                "light")]
             (-> root .-classList (.add system-theme)))
           (-> root .-classList (.add theme))))
       js/undefined)
     [theme])

    ($ (.-Provider theme-provider-context)
       {:value value}
       children)))

(defn use-theme []
  (let [context (react/useContext theme-provider-context)]
    (when (undefined? context)
      (throw (js/Error. "useTheme must be used within a ThemeProvider")))
    {:theme (.-theme context)
     :set-theme (.-setTheme context)}))
