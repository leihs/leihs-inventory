(ns leihs.inventory.client.routes.models.crud.components.manufacturer
  (:require
   ["@/components/ui/popover" :refer [Popover PopoverAnchor PopoverContent]]
   ["@@/button" :refer [Button]]
   ["@@/form" :refer [FormControl FormField FormItem FormLabel]]
   ["@@/input" :refer [Input]]
   ["lucide-react" :refer [Check]]
   ["react-router-dom" :refer [useLoaderData]]
   [clojure.string :as str :refer [lower-case]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defui main [{:keys [form control]}]
  (let [{:keys [manufacturers]} (useLoaderData)
        [open set-open!] (uix/use-state false)
        [width set-width!] (uix/use-state nil)
        get-values (aget form "getValues")
        set-value (aget form "setValue")
        input-ref (uix/use-ref nil)
        list-ref (uix/use-ref nil)

        handle-key-down (fn [e]
                          (let [key (.. e -key)]

                            (cond
                              (and (not (= key "Escape"))
                                   (not (= key "Tab"))
                                   (not (= key "ArrowDown")))
                              (set-open! true)

                              (= key "ArrowDown")
                              (do
                                (.preventDefault e)
                                (.. list-ref -current -firstChild -firstChild (focus)))

                              (and (= key "Tab") open)
                              (do
                                (.preventDefault e)
                                (.. list-ref -current -firstChild -firstChild (focus)))

                              :else
                              nil)))

        handle-key-down-list (fn [e]
                               (let [key (.. e -key)]
                                 (cond
                                   (= key "ArrowUp")
                                   (do
                                     (.preventDefault e)
                                     (.. e -target -parentElement -previousElementSibling -firstChild (focus)))

                                   (= key "ArrowDown")
                                   (do
                                     (.preventDefault e)
                                     (.. e -target -parentElement -nextElementSibling -firstChild (focus)))

                                   :else
                                   nil)))]

    (uix/use-effect
     (fn []
       (when (.. input-ref -current)
         (set-width! (.. input-ref -current -offsetWidth))))
     [])

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
                                   ($ Input (merge {:auto-complete "off"
                                                    :on-key-down handle-key-down}

                                                   (:field (jc %)))))))

                          ($ PopoverContent {:class-name "p-0"
                                             :on-key-down handle-key-down-list
                                             :onOpenAutoFocus (fn [e] (.. e (preventDefault)))
                                             :onCloseAutoFocus (fn [_] (.. input-ref -current -firstChild (focus)))
                                             :onInteractOutside (fn [_]
                                                                  (set-open! false)
                                                                  (.. input-ref -current -firstChild (focus)))
                                             :onEscapeKeyDown (fn []
                                                                (set-open! false)
                                                                (.. input-ref -current -firstChild (focus)))
                                             :style {:width (str width "px")}}

                             ($ :ul {:ref list-ref}
                                (for [manufacturer manufacturers]
                                  (when (str/includes? (str/lower-case manufacturer)
                                                       (str/lower-case (get-values "manufacturer")))
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
