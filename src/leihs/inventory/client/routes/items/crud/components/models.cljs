(ns leihs.inventory.client.routes.items.crud.components.models
  (:require
   ["@/components/ui/command" :refer [Command CommandEmpty CommandGroup
                                      CommandInput CommandItem CommandList]]
   ["@/components/ui/popover" :refer [Popover PopoverContent PopoverTrigger]]
   ["@@/button" :refer [Button]]
   ["@@/form" :refer [FormControl FormField FormItem FormLabel]]
   ["lucide-react" :refer [Check ChevronsUpDown]]
   ["react-router-dom" :as router]
   [clojure.string :as str]
   [leihs.inventory.client.lib.utils :refer [cj]]
   [uix.core :as uix :refer [$ defui]]))

(defui main [{:keys [control form block]}]
  (let [models (:models (router/useLoaderData))
        params (router/useParams)
        model-id (aget params "model-id")
        has-model-id (boolean model-id)
        [open set-open!] (uix/use-state false)
        [width set-width!] (uix/use-state nil)
        buttonRef (uix/use-ref nil)
        set-value (aget form "setValue")]

    (uix/use-effect
     (fn []
       (when has-model-id
         (let [name (when model-id
                      (some (fn [model]
                              (when (= (:model_id model) model-id)
                                (:product model)))
                            models))]

           (set-value "models" #js {:id model-id
                                    :name name}))))
     [has-model-id model-id set-value models])

    (uix/use-effect
     (fn []
       (when (.. buttonRef -current)
         (set-width! (.. buttonRef -current -offsetWidth))))
     [])

    ($ FormField {:control (cj control)
                  :name (:name block)
                  :render #($ FormItem {:class-name "flex flex-col mt-6"}
                              ($ FormLabel (:label block))

                              (let [id (aget % "field" "value" "id")
                                    name (aget % "field" "value" "name")]

                                ($ Popover {:open open
                                            :onOpenChange set-open!}
                                   ($ PopoverTrigger {:asChild true}
                                      ($ FormControl
                                         ($ Button (merge (:props block)
                                                          {:ref buttonRef
                                                           :on-click (fn [] (set-open! (not open)))
                                                           :variant "outline"
                                                           :role "cobobox"
                                                           :name (:name block)
                                                           :disabled has-model-id
                                                           :class-name "justify-between"})

                                            (if (not= name "")
                                              name
                                              ($ :span {:class-name "text-muted-foreground"}
                                                 "Select option"))
                                            ($ ChevronsUpDown {:class-name "ml-auto h-4 w-4 opacity-50"}))))

                                   ($ PopoverContent {:class-name "p-0"
                                                      :style {:width (str width "px")}}
                                      ($ Command
                                         {:filter (fn [value search]
                                                    (let [lSearch (str/lower-case search)
                                                          lValue (str/lower-case value)]
                                                      (if (str/includes? lValue lSearch) 1 0)))}
                                         ($ CommandInput {:placeholder "Search models"})
                                         ($ CommandList
                                            ($ CommandEmpty "No Model Found")
                                            ($ CommandGroup
                                               (for [model models]
                                                 ($ CommandItem {:value (:product model)
                                                                 :key (:model_id model)
                                                                 :onSelect (fn [] (set-value
                                                                                   (:name block)
                                                                                   #js {:id (:model_id model)
                                                                                        :name (:product model)})
                                                                             (set-open! false))}

                                                    ($ Check
                                                       {:class-name (str "mr-2 h-4 w-4 "
                                                                         (if (= (:model_id model) id)
                                                                           "visible"
                                                                           "invisible"))})
                                                    (:product model))))))))))})))

(def Models
  (uix/as-react
   (fn [props]
     (main props))))
