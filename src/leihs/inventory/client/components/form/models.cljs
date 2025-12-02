(ns leihs.inventory.client.components.form.models
  (:require
   ["@/components/ui/command" :refer [Command CommandEmpty CommandInput
                                      CommandItem CommandList]]
   ["@/components/ui/popover" :refer [Popover PopoverContent PopoverTrigger]]
   ["@@/button" :refer [Button]]
   ["@@/dialog" :refer [Dialog DialogContent DialogHeader
                        DialogTitle DialogTrigger]]

   ["@@/form" :refer [FormField FormLabel FormItem
                      FormMessage FormDescription]]
   ["@@/spinner" :refer [Spinner]]
   ["@@/table" :refer [Table TableBody TableCell TableRow]]
   ["lucide-react" :refer [Check ChevronsUpDown Image Trash Loader2Icon]]
   ["react-hook-form" :as hook-form]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router]
   [leihs.inventory.client.lib.client :refer [http-client]]
   [leihs.inventory.client.lib.hooks :as hooks]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defn check-path-existing [id items]
  (some (fn [item]
          (= id (:id item)))
        items))

(defn find-index-from-id [id items]
  (some (fn [[idx item]]
          (when (= id (:id item))
            idx))
        (map-indexed vector items)))

(defui main [{:keys [form name props label children]}]
  (let [[t] (useTranslation)

        control (cj (.-control form))

        params (router/useParams)
        path (router/generatePath "/inventory/:pool-id/models" params)

        [open set-open!] (uix/use-state false)
        [width set-width!] (uix/use-state nil)
        [data set-data!] (uix/use-state [])

        [search set-search!] (uix/use-state "")
        debounced-search (hooks/use-debounce search 200)

        [loading? set-loading!] (uix/use-state false)
        buttonRef (uix/use-ref nil)

        handle-open-change (fn [val]
                             (set-search! "")
                             (set-data! [])
                             (set-open! val))

        {:keys [fields append remove update]} (jc (hook-form/useFieldArray
                                                   (cj {:control control
                                                        :keyName (str name "-id")
                                                        :name name})))]

    (uix/use-effect
     (fn []
       (when (> (count debounced-search) 2)
         (let [fetch (fn []
                       (set-loading! true)
                       (-> http-client
                           (.get (str path "/" "?search=" debounced-search)
                                 #js {:cache false})
                           (.then (fn [response]
                                    (let [data (jc (.-data response))]
                                      (set-loading! false)
                                      (set-data! data))))
                           (.catch
                            (fn [err]
                              (js/console.error "Error fetching result" err)))))]
           (fetch))))
     [debounced-search path])

    (uix/use-effect
     (fn []
       (when (.. buttonRef -current)
         (set-width! (.. buttonRef -current -offsetWidth))))
     [])

    ($ :div {:class-name "flex flex-col gap-2"}
       ($ FormField
          {:control control
           :name name
           :render #($ FormItem
                       (when label
                         ($ FormLabel (t label) (when (:required props) "*")))
                       ($ Popover {:open open
                                   :on-open-change handle-open-change}

                          ($ PopoverTrigger {:as-child true}
                             ($ Button {:variant "outline"
                                        :role "combobox"
                                        :ref buttonRef
                                        :name (:name (:field (jc %)))
                                        :on-click (fn [] (set-open! (not open)))
                                        :class-name "w-full justify-between"}
                                (t (-> props :text :select))
                                ($ ChevronsUpDown {:class-name "ml-2 h-4 w-4 shrink-0 opacity-50"})))

                          ($ PopoverContent {:class-name "p-0"
                                             :style {:width (str width "px")}}

                             ($ Command {:should-filter false
                                         :on-change (fn [e] (set-search! (.. e -target -value)))}

                                ($ CommandInput {:placeholder (t (-> props :text :placeholder))
                                                 :data-test-id "models-input"})

                                ($ CommandList {:data-test-id "models-list"}
                                   (if loading?
                                     ($ Spinner {:className "absolute right-0 top-0 m-3"})
                                     ($ CommandEmpty (cond
                                                       loading?
                                                       (t (-> props :text :searching))

                                                       (empty? data)
                                                       (t (-> props :text :search_empty))

                                                       :else
                                                       (t (-> props :text :not_found)))))

                                   (for [element data]
                                     ($ CommandItem {:key (:id element)
                                                     :value (:id element)
                                                     :keywords #js [(:name element)]
                                                     :on-select (fn []
                                                                  (set-open! false)
                                                                  (if
                                                                   (not (check-path-existing (:id element) fields))
                                                                    (append (cj (merge {:product (:product element)
                                                                                        :version (:version element)
                                                                                        :name (:name element)
                                                                                        :url (:url element)
                                                                                        :id (:id element)}
                                                                                       (into {}
                                                                                             (map (fn [attr]
                                                                                                    (when-let [value (get element (keyword attr))]
                                                                                                      [(keyword attr) value]))
                                                                                                  (:attributes props))))))
                                                                    (remove (find-index-from-id (:id element) fields))))}

                                        ($ Check
                                           {:class-name (str "mr-2 h-4 w-4 "
                                                             (if (check-path-existing (:id element) fields)
                                                               "visible"
                                                               "invisible"))})
                                        ($ :button {:type "button"
                                                    :class-name (str (when (= 1 (:level element)) " font-bold ")
                                                                     (when (= 2 (:level element)) " font-medium ")
                                                                     " truncate")}
                                           (str (:name element)))))))))
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
                                    ($ Button {:variant "outline"
                                               :data-test-id (str (:product field) "-preview")
                                               :class-name "p-0 w-10 h-10 hover:bg-white shadow-none align-middle"}
                                       ($ :img {:src (str (:url field) "/thumbnail")
                                                :class-name "w-10 h-10 p-1 object-contain rounded"})))
                                 ($ DialogContent
                                    ($ DialogHeader
                                       ($ DialogTitle (:name field)))
                                    ($ :img {:src (:url field)
                                             :class-name "w-[50vh] aspect-square object-contain"})))
                              ($ Image {:class-name "w-10 h-10 scale-[1.2] align-middle"})))

                         ($ TableCell {:class-name ""}
                            (str (:name field)))

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
