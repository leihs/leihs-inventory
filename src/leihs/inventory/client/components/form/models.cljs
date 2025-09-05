(ns leihs.inventory.client.components.form.models
  (:require
   ["@/components/ui/command" :refer [Command CommandEmpty CommandInput
                                      CommandItem CommandList]]
   ["@/components/ui/popover" :refer [Popover PopoverContent PopoverTrigger]]
   ["@@/button" :refer [Button]]
   ["@@/dialog" :refer [Dialog DialogContent DialogHeader
                        DialogTitle DialogTrigger]]
   ["@@/form" :refer [FormField FormItem FormMessage FormDescription]]
   ["@@/label" :refer [Label]]
   ["@@/table" :refer [Table TableBody TableCell TableRow]]
   ["lucide-react" :refer [Check ChevronsUpDown Image Trash Loader2Icon]]
   ["react-hook-form" :as hook-form]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router :refer [useParams]]
   [leihs.inventory.client.lib.client :refer [http-client]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
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

(defui main [{:keys [form name props label children]}]
  (let [[t] (useTranslation)

        control (cj (.-control form))

        params (useParams)
        path (router/generatePath "/inventory/:pool-id/models" params)

        [open set-open!] (uix/use-state false)
        [width set-width!] (uix/use-state nil)
        [search set-search!] (uix/use-state "")
        [data set-data!] (uix/use-state [])

        [pending set-pending!] (uix/use-state false)

        buttonRef (uix/use-ref nil)

        {:keys [fields append remove update]} (jc (hook-form/useFieldArray
                                                   (cj {:control control
                                                        :keyName (str name "-id")
                                                        :name name})))]

    (uix/use-effect
     (fn []
       (if (< (count search) 3)
         (set-data! [])
         (let [debounce (js/setTimeout
                         (fn []
                           (set-pending! true)
                           ;; Fetch result based on the search term
                           (-> http-client
                               (.get (str path "/" "?search=" search))
                               (.then (fn [res]
                                        (let [data (jc (.-data res))]
                                          (set-pending! false)
                                          (set-data! data))))
                               (.catch
                                (fn [err]
                                  (js/console.error "Error fetching result" err)))))
                         200)]

           (set-pending! false)
           (fn [] (js/clearTimeout debounce)))))
     [search props path name])

    (uix/use-effect
     (fn []
       (when (.. buttonRef -current)
         (set-width! (.. buttonRef -current -offsetWidth))))
     [])

    ($ :div {:class-name "flex flex-col gap-2"}
       ($ FormField {:control control
                     :name name
                     :render #($ FormItem
                                 ($ Label (t label) (when (:required props) "*"))
                                 ($ Popover {:open open
                                             :on-open-change (fn [val]
                                                               (set-search! "")
                                                               (set-data! [])
                                                               (set-open! val))}

                                    ($ PopoverTrigger {:as-child true}
                                       ($ Button {:variant "outline"
                                                  :role "combobox"
                                                  :ref buttonRef
                                                  :on-click (fn [] (set-open! (not open)))
                                                  :class-name "w-full justify-between"}
                                          (t (-> props :text :select))
                                          ($ ChevronsUpDown {:class-name "ml-2 h-4 w-4 shrink-0 opacity-50"})))

                                    ($ PopoverContent {:class-name "p-0"
                                                       :style {:width (str width "px")}}

                                       ($ Command {:should-filter false
                                                   :on-change (fn [event] (set-search! (.. event -target -value)))}
                                          ($ :div
                                             ($ CommandInput
                                                {:placeholder (t (-> props :text :placeholder))})
                                             (when pending
                                               ($ Loader2Icon {:className "absolute right-0 top-0 h-4 w-4 m-3 animate-spin opacity-50"})))
                                          ($ CommandList {:data-test-id "models-list"}

                                             ($ CommandEmpty (cond
                                                               pending
                                                               (t (-> props :text :searching))

                                                               (< (count search) 3)
                                                               (t (-> props :text :search_empty))

                                                               :else
                                                               (t (-> props :text :not_found))))

                                             (for [element data]
                                               ($ CommandItem {:key (:id element)
                                                               :value (str (:product element) " " (:version element))
                                                               :on-select (fn []
                                                                            (set-open! false)
                                                                            (if
                                                                             (not (check-path-existing (:product element) fields))
                                                                              (append (cj (merge {:product (:product element)
                                                                                                  :version (:version element)
                                                                                                  :url (:url element)
                                                                                                  :id (:id element)}
                                                                                                 (into {}
                                                                                                       (map (fn [attr]
                                                                                                              (when-let [value (get element (keyword attr))]
                                                                                                                [(keyword attr) value]))
                                                                                                            (:attributes props))))))
                                                                              (remove (find-index-from-path (:product element) fields))))}

                                                  ($ Check
                                                     {:class-name (str "mr-2 h-4 w-4 "
                                                                       (if (check-path-existing (:product element) fields)
                                                                         "visible"
                                                                         "invisible"))})
                                                  ($ :span
                                                     {:class-name (str (when (= 1 (:level element)) " font-bold ")
                                                                       (when (= 2 (:level element)) " font-medium ")
                                                                       " truncate")}
                                                     (str (:product element) " " (:version element)))))))))
                                 ($ FormDescription)
                                 ($ FormMessage))})

       (when (not-empty fields)
         ($ :div {:class-name "rounded-md border overflow-hidden"}
            ($ Table {:class-name "w-full"}

               ($ TableBody
                  (doall
                   (map-indexed
                    (fn [index field]
                      ($ TableRow {:class-name ""
                                   :key index}

                         ($ TableCell {:class-name "w-0"}

                            (if (:url field)
                              ($ Dialog
                                 ($ DialogTrigger {:as-child true}
                                    ($ Button {:variant "ghost"
                                               :data-test-id (str (:product field) "-preview")
                                               :class-name "p-0"}
                                       ($ :img {:src (str (:url field) "/thumbnail")
                                                :class-name "min-w-10 h-10 object-cover rounded-sm"})))
                                 ($ DialogContent {:class-name "max-w-3xl"}
                                    ($ DialogHeader
                                       ($ DialogTitle (:product field)))
                                    ($ :img {:src (:url field)
                                             :class-name "w-full h-auto object-contain"})))
                              ($ Image {:class-name "w-10 h-10 scale-[1.2]"})))

                         ($ TableCell {:class-name ""} (str (:product field) " " (:version field)))

                         (when children ($ :<> (children update index field)))

                         ($ TableCell {:class-name "text-right w-0"}
                            ($ Button {:variant "outline"
                                       :type "button"
                                       :on-click #(remove index)
                                       :size "icon"}
                               ($ Trash {:class-name "h-4 w-4"})))))
                    fields)))))))))

(def Models
  (uix/as-react
   (fn [props]
     (main props))))
