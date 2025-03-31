(ns leihs.inventory.client.routes.models.components.forms.category-assignment
  (:require
   ["@@/button" :refer [Button]]
   ["@@/command" :refer [Command CommandEmpty CommandInput CommandItem
                         CommandList]]
   ["@@/popover" :refer [Popover PopoverContent PopoverTrigger]]
   ["@@/table" :refer [Table TableBody TableCell TableRow]]
   ["lucide-react" :refer [Check ChevronsUpDown Trash]]
   ["react-hook-form" :as hook-form]
   ["react-router-dom" :refer [useLoaderData]]
   [clojure.string :as str]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defn concat-if-multiple [v]
  (if (> (count v) 1)
    (str/join " / " v)
    (first v)))

(defn check-path-existing [path items]
  (some (fn [item]
          (= path (:path item)))
        items))

(defn find-index-from-path [path items]
  (some (fn [[idx item]]
          (when (= path (:path item))
            idx))
        (map-indexed vector items)))

(defn find-by-id [list id]
  (when (seq list)
    (some #(when (= id (:id %)) %) list)))

(defui main [{:keys [control props]}]
  (let [{:keys [categories]} (useLoaderData)
        [open set-open!] (uix/use-state false)
        [width set-width!] (uix/use-state nil)
        buttonRef (uix/use-ref nil)

        {:keys [fields append remove]} (jc (hook-form/useFieldArray
                                            (cj {:control control
                                                 :keyName "field_id"
                                                 :name "categories"})))

        [flat-categories set-flat-categories!] (uix/use-state [])

        items (uix/use-memo
               (fn []
                 (let [flattened-categories* (atom [])]
                   (letfn [(flatten-categories [path items level]
                             (loop [remaining-items items]
                               (when (seq remaining-items)
                                 (let [category (first remaining-items)]
                                   (swap! flattened-categories* conj
                                          {:id (:category_id category)
                                           :path (if (nil? path)
                                                   (:name category)
                                                   (concat-if-multiple (conj [path] (:name category))))
                                           :label (:name category)
                                           :level level})

                                   (when (seq (:children category))
                                     (let [path (if (nil? path)
                                                  (:name category)
                                                  (concat-if-multiple (conj [path] (:name category))))]
                                       (flatten-categories path (:children category) (inc level))))

                                   (recur (rest remaining-items))))))]

                     (flatten-categories nil (:children categories) 1)
                     @flattened-categories*)

                   (set-flat-categories! @flattened-categories*)))
               [categories])]

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
                "Select categories"
                ($ ChevronsUpDown {:class-name "ml-2 h-4 w-4 shrink-0 opacity-50"})))

          ($ PopoverContent {:class-name "p-0"
                             :style {:width (str width "px")}}
             ($ Command
                {:filter (fn [value search]
                           (if (str/includes?
                                (str/lower-case value)
                                (str/lower-case search))
                             1 0))}

                ($ CommandInput {:placeholder "Search item..."})
                ($ CommandList

                   ($ CommandEmpty "No item found.")

                   (for [item flat-categories]
                     ($ CommandItem {:key (:path item)
                                     :value (:label item)
                                     :on-select #(do (set-open! false)
                                                     (js/console.debug item)
                                                     (if
                                                      (not (check-path-existing (:path item) fields))
                                                       (append (cj (assoc item
                                                                          :type "Category"
                                                                          :name (:label item))))
                                                       (remove (find-index-from-path (:path item) fields))))
                                     :style {:padding-left (str (* (:level item) 16) "px")}}

                        ($ Check
                           {:class-name (str "mr-2 h-4 w-4 "
                                             (if (check-path-existing (:path item) fields)
                                               "visible"
                                               "invisible"))})
                        ($ :span
                           {:class-name (str (when (= 1 (:level item)) " font-bold ")
                                             (when (= 2 (:level item)) " font-medium ")
                                             " truncate")}
                           (:label item))))))))

       (when (not-empty fields)
         ($ :div {:class-name "rounded-md border overflow-hidden"}
            ($ Table {:class-name "w-full"}

               ($ TableBody
                  (doall
                   (map-indexed
                    (fn [index field]
                      ($ TableRow {:class-name "" :key index}

                         ($ TableCell {:class-name ""} (if (:path field)
                                                         (:path field)
                                                         (:path (find-by-id flat-categories (:id field)))))

                         ($ TableCell {:class-name "flex gap-2 justify-end"}
                            ($ Button {:variant "outline"
                                       :type "button"
                                       :on-click #(remove index)
                                       :size "icon"}
                               ($ Trash {:class-name "h-4 w-4"})))))
                    fields)))))))))

(def CategoryAssignment
  (uix/as-react
   (fn [props]
     (main props))))
