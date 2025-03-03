(ns leihs.inventory.client.routes.models.components.forms.compatible-models
  (:require
   ["@/components/ui/command" :refer [Command CommandEmpty CommandInput
                                      CommandItem CommandList]]
   ["@/components/ui/popover" :refer [Popover PopoverContent PopoverTrigger]]
   ["@@/button" :refer [Button]]
   ["@@/table" :refer [Table TableBody TableCell TableRow]]
   ["lucide-react" :refer [Check ChevronsUpDown Image Trash]]
   ["react-hook-form" :as hook-form]
   [clojure.string :as str]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [leihs.inventory.client.routes.models.create.context :refer [state-context]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defn check-path-existing [product items]
  (some (fn [item]
          (= product (:product item)))
        items))

(defn find-index-from-path [path items]
  (some (fn [[idx item]]
          (when (= path item)
            idx))
        (map-indexed vector items)))

(defui main [{:keys [control models form props]}]
  (let [{:keys [models]} (uix/use-context state-context)
        [open set-open!] (uix/use-state false)
        [width set-width!] (uix/use-state nil)
        buttonRef (uix/use-ref nil)

        {:keys [fields append remove]} (jc (hook-form/useFieldArray
                                            (cj {:control control
                                                 :name "compatibles"})))]

    (uix/use-effect
     (fn []
       (when (.. buttonRef -current)
         (set-width! (.. buttonRef -current -offsetWidth))))
     [])

    ($ :div {:class-name "flex flex-col gap-2"}
       ($ Popover {:open open
                   :on-open-change #(set-open! %)}
          ($ PopoverTrigger {:as-child true}
             ($ Button {:variant "outline"
                        :role "combobox"
                        :ref buttonRef
                        :on-click #(set-open! (not open))
                        :class-name "w-full justify-between"}
                "Select models"
                ($ ChevronsUpDown {:class-name "ml-2 h-4 w-4 shrink-0 opacity-50"})))

          ($ PopoverContent {:class-name "p-0"
                             :style {:width (str width "px")}}
             ($ Command
                {:filter (fn [value search]
                           (let [lSearch (str/lower-case search)
                                 lValue (str/lower-case value)]
                             (if (str/includes? lValue lSearch) 1 0)))}
                ($ CommandInput {:placeholder "Search item..."})
                ($ CommandList

                   ($ CommandEmpty "No item found.")

                   (for [model models]
                     ($ CommandItem {:key (:model_id model)
                                     :value (:product model)
                                     :on-select #(do (set-open! false)
                                                     (if
                                                      (not (check-path-existing (:product model) fields))
                                                       (append (cj {:product (:product model)}))
                                                       (remove (find-index-from-path (:product model) fields))))}

                        ($ Check
                           {:class-name (str "mr-2 h-4 w-4 "
                                             (if (check-path-existing (:product model) fields)
                                               "visible"
                                               "invisible"))})
                        ($ :span
                           {:class-name (str (when (= 1 (:level model)) " font-bold ")
                                             (when (= 2 (:level model)) " font-medium ")
                                             " truncate")}
                           (:product model))))))))

       (when (not-empty fields)
         ($ :div {:class-name "rounded-md border overflow-hidden"}
            ($ Table {:class-name "w-full"}

               ($ TableBody
                  (doall
                   (map-indexed
                    (fn [index field]
                      ($ TableRow {:class-name "" :key index}

                         ($ TableCell {:class-name "w-0"}
                            ($ Image {:class-name "w-10 h-10"}))

                         ($ TableCell {:class-name ""} (:product field))

                         ($ TableCell {:class-name "flex gap-2 justify-end"}
                            ($ Button {:variant "outline"
                                       :type "button"
                                       :on-click #(remove index)
                                       :size "icon"}
                               ($ Trash {:class-name "h-4 w-4"})))))
                    fields)))))))))

(def CompatibleModels
  (uix/as-react
   (fn [props]
     (main props))))
