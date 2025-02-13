(ns leihs.inventory.client.routes.models.create.components.category-assignment
  (:require
   ["@@/button" :refer [Button]]
   ["@@/command" :refer [Command CommandEmpty CommandInput CommandItem CommandList]]
   ["@@/popover" :refer [Popover PopoverContent PopoverTrigger]]
   ["lucide-react" :refer [ChevronsUpDown Check]]
   [clojure.string :as str]
   [leihs.inventory.client.routes.models.create.context :refer [state-context]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defui main [{:keys [control props]}]
  (let [{:keys [categories]} (uix/use-context state-context)
        [open set-open!] (uix/use-state false)
        [value set-value!] (uix/use-state nil)
        items (uix/use-memo
               (fn []
                 (let [flattened-items (atom [])]
                   (letfn [(flatten-category [path items level]
                             (doseq [item items]
                               (if (string? item)
                                 (swap! flattened-items conj
                                        {:value (str/join "-" (conj path item))
                                         :label item
                                         :level level})
                                 (do
                                   (swap! flattened-items conj
                                          {:value (str/join "-" (conj path (:subcategory item)))
                                           :label (:subcategory item)
                                           :level level})
                                   (when (seq (:items item))
                                     (flatten-category (conj path (:subcategory item)) (:items item) (inc level)))))))]
                     (doseq [category categories]
                       (swap! flattened-items conj
                              {:value (:category category)
                               :label (:category category)
                               :level 0})
                       (flatten-category [(:category category)] (:items category) 1))
                     @flattened-items))) [categories])]

    ($ Popover {:open open
                :on-open-change #(set-open!)}
       ($ PopoverTrigger {:as-child true}
          ($ Button {:variant "outline"
                     :role "combobox"
                     :aria-expanded open
                     :class-name "w-[300px] justify-between"}
             (if (seq value)
               (let [item (some #(when (= (:value %) value) %) items)]
                 (:label item))
               "Select item...")
             ($ ChevronsUpDown {:class-name "ml-2 h-4 w-4 shrink-0 opacity-50"})))

       ($ PopoverContent {:class-name "w-[300px] p-0"}
          ($ Command
             ($ CommandInput {:placeholder "Search item..."}
                ($ CommandList
                   ($ CommandEmpty "No item found.")
                   ($ CommandItem "hello")))

             #_($ CommandList nil
                  ($ CommandEmpty nil "No item found.")
                  (for [item items]
                    ($
                     CommandItem
                     {:key (:value item)
                      :value (:value item)
                      :on-select #(let [current-value (if (= % value) "" %)]
                                    (set-value! value current-value)
                                    (set-open! open false))
                      :class-name (str "pl-[" (* (:level item) 12) "px]")}
                     ($ Check
                        {:class-name (str "mr-2 h-4 w-4"
                                          (if (= value (:value item))
                                            "opacity-100"
                                            "opacity-0"))})
                     ($ :span
                        {:class-name (str (when (zero? (:level item)) "font-bold")
                                          (when (= 1 (:level item)) "font-medium")
                                          "truncate")}
                        (:label item))))))))))

(def CategoryAssignment
  (uix/as-react
   (fn [props]
     (main props))))
