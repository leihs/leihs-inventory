(ns leihs.inventory.client.components.form.fields.checkbox-field
  (:require
   ["@@/checkbox" :refer [Checkbox]]
   ["@@/form" :refer [FormControl FormField FormItem FormLabel FormMessage]]
   ["@@/tooltip" :refer [Tooltip TooltipContent TooltipTrigger]]
   ["lucide-react" :refer [Info]]
   [leihs.inventory.client.lib.utils :refer [jc]]
   [uix.core :as uix :refer [$ defui]]))

(defn- info-icon [info]
  ($ Tooltip
     ($ TooltipTrigger {:asChild true}
        ($ :button {:type "button"
                    :class-name "inline-flex items-center text-muted-foreground hover:text-foreground"
                    :aria-label info
                    :on-click (fn [e]
                                (.preventDefault e)
                                (.stopPropagation e))}
           ($ Info {:class-name "h-4 w-4"})))
     ($ TooltipContent {:class-name "max-w-[20rem]"}
        info)))

(defn- label-row [block]
  ($ :div {:class-name "inline-flex items-center gap-1 pl-4"}
     ($ FormLabel (:label block))
     (when (:info block)
       (info-icon (:info block)))))

(defui CheckboxField [{:keys [form block]}]
  ($ FormField {:control (.-control form)
                :name (:name block)
                :render (fn [field-props]
                          (let [field (-> (jc field-props) :field)]
                            ($ FormItem {:class-name "mt-6"}
                               ($ FormControl
                                  ($ Checkbox (merge
                                               {:checked (:value field)
                                                :onCheckedChange (:onChange field)}
                                               (:props block))))
                               (label-row block)
                               ($ FormMessage))))}))
