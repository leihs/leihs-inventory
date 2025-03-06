(ns leihs.inventory.client.routes.models.components.forms.manufacturer
  (:require
   ["@/components/ui/command" :refer [Command CommandEmpty CommandInput
                                      CommandItem CommandList]]
   ["@/components/ui/popover" :refer [Popover PopoverContent PopoverTrigger PopoverAnchor]]
   ["@@/button" :refer [Button]]
   ["@@/form" :refer [FormControl FormField FormItem FormLabel]]
   ["@@/input" :refer [Input]]
   ["lucide-react" :refer [ChevronsUpDown Check]]
   [clojure.string :as str]
   [leihs.inventory.client.lib.utils :refer [jc cj]]
   [leihs.inventory.client.routes.models.create.context :refer [state-context]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defn filter-field [input list]
  (filter (fn [manufacturer] (= manufacturer input)) list))

(defui main [{:keys [form control props]}]
  (let [{:keys [manufacturers]} (uix/use-context state-context)
        [open set-open!] (uix/use-state false)
        [width set-width!] (uix/use-state nil)
        [input set-input!] (uix/use-state nil)
        [filtered-manufacturers set-filtered-manufacturers!] (uix/use-state nil)
        get-values (aget form "getValues")
        set-value (aget form "setValue")
        field-val (get-values "manufacturer")
        input-ref (uix/use-ref nil)
        list-ref (uix/use-ref nil)

        handle-change (fn [e] (js/console.debug (.. e -target -value)))]

    (uix/use-effect
     (fn []
       (when (not (= input nil))
         (set-filtered-manufacturers!
          (filter (fn [manufacturer] (= manufacturer input)) manufacturers)))

       (when (= input "")
         (set-filtered-manufacturers! nil)))

     [input manufacturers])

    (uix/use-effect
     (fn []
       (when (.. input-ref -current)
         (set-width! (.. input-ref -current -offsetWidth))))
     [])

    (uix/use-effect
     (fn []
       (js/console.debug "field-val" field-val))
     [field-val])

    ($ :div {:class-name "flex flex-col gap-2"}
       ($ FormField
          {:name "manufacturer"
           :control (cj control)
           :render #($ FormItem {:class-name "flex flex-col mt-6"}
                       ($ FormLabel "Manufacturer")

                       ($ Popover {:open open}
                          ($ PopoverAnchor {:asChild true}
                             ($ :div {:ref input-ref}
                                ($ FormControl
                                   ($ Input (merge {:on-focus (fn [] (set-open! true))
                                                    :on-key-down (fn [e] (when (and (= (.. e -key) "Tab") open)
                                                                           (.preventDefault e)
                                                                           (.. list-ref -current -firstChild -firstChild (focus))))}
                                                   (:field (jc %)))))))

                          ($ PopoverContent {:class-name "p-0"
                                             :onOpenAutoFocus (fn [e] (.. e (preventDefault)))
                                             :onEscapeKeyDown (fn [] (set-open! false))
                                             :style {:width (str width "px")}}

                             ($ :ul {:ref list-ref}
                                (for [manufacturer manufacturers]
                                  (when (str/includes? manufacturer (get-values "manufacturer"))
                                    ($ :li {:key manufacturer}
                                       ($ Button {:variant "ghost"
                                                  :class-name "w-full justify-start"
                                                  :on-click (fn []
                                                              (set-open! false)
                                                              (if (= (str (get-values "manufacturer")) manufacturer)
                                                                (set-value "manufacturer" nil)
                                                                (set-value "manufacturer" manufacturer)))}

                                          ($ Check
                                             {:class-name (str "mr-2 h-4 w-4 "
                                                               (if (= manufacturer (get-values "manufacturer"))
                                                                 "visible"
                                                                 "invisible"))})
                                          ($ :span manufacturer)))))))))}))))

(def Manufacturer
  (uix/as-react
   (fn [props]
     (main props))))
