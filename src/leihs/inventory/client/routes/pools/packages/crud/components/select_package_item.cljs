(ns leihs.inventory.client.routes.pools.packages.crud.components.select-package-item
  (:require
   ["@/components/ui/command" :refer [Command CommandEmpty CommandInput
                                      CommandItem CommandList]]
   ["@/components/ui/popover" :refer [Popover PopoverContent PopoverTrigger]]
   ["@/components/ui/tooltip" :refer [Tooltip TooltipContent TooltipTrigger]]
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

(defn item-display-name [item]
  (if (:inventory_code item)
    (str (:inventory_code item) " - " (:model_name item))
    (:model_name item)))

(defui DropdownItem [{:keys [data fields on-select on-selected]}]
  (let [[selected? set-selected!] (uix/use-state false)
        ref (uix/use-ref nil)
        callback (fn [mutations]
                   (doseq [mutation mutations]
                     (let [selected (.. mutation -target -dataset -selected)]
                       (when (= "attributes" (.-type mutation))
                         (set-selected! (parse-boolean selected))))))]

    (hooks/use-mutation-observer {:ref ref
                                  :callback callback
                                  :options {:attributes true
                                            :attributeFilter #js ["data-selected"]}})

    (uix/use-effect
     (fn []
       (on-selected selected?))
     [selected? on-selected])

    ($ CommandItem {:ref ref
                    :value (:id data)
                    :keywords #js [(:inventory_code data) (:model_name data)]
                    :on-select on-select}
       ($ Check
          {:class-name (str "mr-2 h-4 w-4 "
                            (if (check-path-existing (:id data) fields)
                              "visible"
                              "invisible"))})
       ($ :span {:class-name "truncate"}
          (item-display-name data)))))

;; Select component - handles search and selection UI
(defui SelectPackageItem [{:keys [name props]}]
  (let [[t] (useTranslation)
        params (router/useParams)
        path (router/generatePath "/inventory/:pool-id/items/" params)

        [open set-open!] (uix/use-state false)
        [width set-width!] (uix/use-state nil)
        [data set-data!] (uix/use-state [])
        [selected set-selected!] (uix/use-state nil)

        [search set-search!] (uix/use-state "")
        debounced-search (hooks/use-debounce search 300)
        size (hooks/use-window-size)

        {:keys [fields append remove]} (use-array-items)

        [loading? set-loading!] (uix/use-state false)
        buttonRef (uix/use-ref nil)

        handle-select (fn []
                        (set-open! false)
                        (if (not (check-path-existing (:id selected) fields))
                          (append (cj {:id (:id selected)
                                       :inventory_code (:inventory_code selected)
                                       :model_name (:model_name selected)
                                       :url (:url selected)}))
                          (remove (find-index-from-id (:id selected) fields))))

        handle-open-change (fn [val]
                             (set-selected! nil)
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
                           (.get (safe-query (str path "/") {:search_term debounced-search
                                                             :for_package true})
                                 #js {:cache false})
                           (.then (fn [response]
                                    (let [result (jc (.-data response))]
                                      (set-loading! false)
                                      (set-data! result))))
                           (.catch
                            (fn [err]
                              (js/console.error "Error fetching items" err)
                              (set-loading! false)))))]
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
          ($ Button {:variant "outline"
                     :role "combobox"
                     :ref buttonRef
                     :name name
                     :on-click (fn [] (set-open! (not open)))
                     :class-name "w-full justify-between"}
             (t (-> props :text :select))
             ($ ChevronsUpDown {:class-name "ml-2 h-4 w-4 shrink-0 opacity-50"})))

       ($ PopoverContent {:class-name "p-0"
                          :style {:width (str width "px")}}

          ($ Command {:should-filter false
                      :on-change (fn [e] (set-search! (.. e -target -value)))}

             ($ CommandInput {:placeholder (t (-> props :text :search))
                              :data-test-id "items-input"})

             ($ CommandList {:data-test-id "items-list"
                             :on-scroll (fn [] (set-selected! nil))}
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
                  ($ Tooltip {:key (:id element)
                              :open (= (:id selected)
                                       (:id element))}

                     ($ TooltipTrigger {:as-child true}
                        ($ :div
                           ($ DropdownItem
                              {:key (:id element)
                               :data element
                               :fields (jc fields)
                               :on-selected (fn [selected?]
                                              (when selected?
                                                (set-selected! element)))
                               :on-select handle-select})))

                     (when (:url selected)
                       ($ TooltipContent {:side "top"
                                          :alignOffset 10
                                          :align "end"}
                          ($ :img {:src (:url selected)
                                   :alt (item-display-name selected)
                                   :class-name "w-32 h-32 object-contain"})))))))))))
