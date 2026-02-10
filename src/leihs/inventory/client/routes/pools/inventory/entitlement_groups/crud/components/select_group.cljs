(ns leihs.inventory.client.routes.pools.inventory.entitlement-groups.crud.components.select-group
  (:require
   ["@/components/ui/command" :refer [Command CommandEmpty CommandInput
                                      CommandItem CommandList]]
   ["@/components/ui/popover" :refer [Popover PopoverContent PopoverTrigger]]
   ["@@/button" :refer [Button]]
   ["@@/form" :refer [FormControl]]
   ["@@/spinner" :refer [Spinner]]
   ["lucide-react" :refer [Check ChevronsUpDown]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router]
   [leihs.inventory.client.components.form.form-field-array :refer [use-array-items]]
   [leihs.inventory.client.lib.client :refer [http-client safe-query]]
   [leihs.inventory.client.lib.hooks :as hooks]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [uix.core :as uix :refer [$ defui]]))

(defn check-path-existing [id items]
  (some (fn [item]
          (= id (:id item)))
        items))

(defn find-index-from-id [id items]
  (some (fn [[idx item]]
          (when (= id (:id item))
            idx))
        (map-indexed vector items)))

;; Select component - handles search and selection UI
(defui SelectGroup [{:keys [name props]}]
  (let [[t] (useTranslation)
        params (router/useParams)
        path (router/generatePath "/inventory/:pool-id/groups" params)

        [open set-open!] (uix/use-state false)
        [width set-width!] (uix/use-state nil)
        [data set-data!] (uix/use-state [])

        [search set-search!] (uix/use-state "")
        debounced-search (hooks/use-debounce search 200)
        size (hooks/use-window-size)

        {:keys [fields append remove]} (use-array-items)

        [loading? set-loading!] (uix/use-state false)
        buttonRef (uix/use-ref nil)

        handle-select (fn [selected]
                        (set-open! false)
                        (if (not (check-path-existing (:id selected) fields))
                          (append (cj (merge {:name (:name selected)
                                              :url (:url selected)
                                              :id (:id selected)}
                                             (into {}
                                                   (map (fn [attr]
                                                          (when-let [value (get selected (keyword attr))]
                                                            [(keyword attr) value]))
                                                        (:attributes props))))))
                          (remove (find-index-from-id (:id selected) fields))))

        handle-open-change (fn [val]
                             (set-search! "")
                             (set-data! [])
                             (set-open! val))]

    (uix/use-effect
     (fn []
       (when (< (count debounced-search) 2)
         (set-data! []))

       (when (> (count debounced-search) 1)
         (let [fetch (fn []
                       (set-loading! true)
                       (-> http-client
                           (.get (safe-query (str path "/") {:search debounced-search})
                                 #js {:cache false})
                           (.then (fn [response]
                                    (let [data (jc (.-data response))]
                                      (set-loading! false)
                                      (set-data! data))))
                           (.catch
                            (fn [err]
                              (js/console.error "Error fetching groups" err)))))]
           (fetch))))
     [debounced-search path])

    (uix/use-effect
     (fn []
       (when (.. buttonRef -current)
         (set-width! (.. buttonRef -current -offsetWidth))))
     [size])

    ($ Popover {:open open
                :on-open-change handle-open-change}

       ($ PopoverTrigger {:as-child true}
          ($ FormControl
             ($ Button {:variant "outline"
                        :role "combobox"
                        :ref buttonRef
                        :name name
                        :on-click (fn [] (set-open! (not open)))
                        :class-name "w-full justify-between"}
                (t (-> props :text :select))
                ($ ChevronsUpDown {:class-name "ml-2 h-4 w-4 shrink-0 opacity-50"}))))

       ($ PopoverContent {:class-name "p-0"
                          :style {:width (str width "px")}}

          ($ Command {:should-filter false
                      :on-change (fn [e] (set-search! (.. e -target -value)))}

             ($ CommandInput {:placeholder (t (-> props :text :placeholder))
                              :data-test-id "groups-input"})

             ($ CommandList {:data-test-id "groups-list"}
                (when loading?
                  ($ Spinner {:className "absolute right-0 top-0 m-3"}))
                ($ CommandEmpty (cond
                                  loading?
                                  (t (-> props :text :searching))

                                  (< (count search) 2)
                                  (t (-> props :text :search_empty))

                                  (empty? data)
                                  (t (-> props :text :not_found))))

                (for [element data]
                  ($ CommandItem {:key (:id element)
                                  :value (:name element)
                                  :on-select (fn [] (handle-select element))}
                     ($ Check
                        {:class-name (str "mr-2 h-4 w-4 "
                                          (if (check-path-existing (:id element) fields)
                                            "visible"
                                            "invisible"))})
                     ($ :span
                        {:class-name (str (when (= 1 (:level element)) " font-bold ")
                                          (when (= 2 (:level element)) " font-medium ")
                                          " truncate")}
                        (:name element))))))))))
