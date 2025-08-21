(ns leihs.inventory.client.routes.pools.models.crud.components.compatible-models
  (:require
   ["@/components/ui/command" :refer [Command CommandEmpty CommandInput
                                      CommandItem CommandList]]
   ["@/components/ui/popover" :refer [Popover PopoverContent PopoverTrigger]]
   ["@@/button" :refer [Button]]
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

(defui main [{:keys [form name props]}]
  (let [[t] (useTranslation)

        control (cj (.-control form))

        params (useParams)
        path (router/generatePath "/inventory/:pool-id/models" params)

        [open set-open!] (uix/use-state false)
        [width set-width!] (uix/use-state nil)
        [search set-search!] (uix/use-state "")
        [result set-result!] (uix/use-state [])

        [pending set-pending!] (uix/use-state false)

        buttonRef (uix/use-ref nil)

        {:keys [fields append remove]} (jc (hook-form/useFieldArray
                                            (cj {:control control
                                                 :name "compatibles"})))]

    (uix/use-effect
     (fn []
       (if (< (count search) 3)
         (set-result! [])
         (let [debounce (js/setTimeout
                         (fn []
                           (set-pending! true)
                               ;; Fetch result based on the search term
                           (-> http-client
                               (.get (str path "/" "?search=" search))
                               (.then (fn [res]
                                        (let [data (jc (.-data res))]
                                          (set-pending! false)
                                          (set-result! data))))
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
       ($ Popover {:open open
                   :on-open-change #(do
                                      (set-search! "")
                                      (set-result! [])
                                      (set-open! %))}
          ($ PopoverTrigger {:as-child true}
             ($ Button {:variant "outline"
                        :role "combobox"
                        :ref buttonRef
                        :on-click #(set-open! (not open))
                        :class-name "w-full justify-between"}
                (t "pool.model.compatible_models.blocks.compatible_models.select")
                ($ ChevronsUpDown {:class-name "ml-2 h-4 w-4 shrink-0 opacity-50"})))

          ($ PopoverContent {:class-name "p-0"
                             :style {:width (str width "px")}}

             ($ Command {:should-filter false
                         :on-change #(set-search! (.. % -target -value))}
                ($ :div
                   ($ CommandInput {:placeholder (t "pool.model.compatible_models.blocks.compatible_models.search")})
                   (when pending
                     ($ Loader2Icon {:className "absolute right-0 top-0 h-4 w-4 m-3 animate-spin opacity-50"})))
                ($ CommandList {:data-test-id "compatible-models-list"}

                   ($ CommandEmpty (t "pool.model.compatible_models.blocks.compatible_models.not_found"))

                   (for [element result]
                     ($ CommandItem {:key (:id element)
                                     :value (str (:product element) " " (:version element))
                                     :on-select #(do (set-open! false)
                                                     (if
                                                      (not (check-path-existing (:product element) fields))
                                                       (append (cj {:product (:product element)
                                                                    :version (:version element)
                                                                    :cover_image_url (:cover_image_url element)
                                                                    :id (:id element)}))
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

       (when (not-empty fields)
         ($ :div {:class-name "rounded-md border overflow-hidden"}
            ($ Table {:class-name "w-full"}

               ($ TableBody
                  (doall
                   (map-indexed
                    (fn [index field]
                      ($ TableRow {:class-name "" :key index}

                         ($ TableCell {:class-name "w-0"}
                            (if (:cover_image_url field)
                              ($ :img {:src (:cover_image_url field)
                                       :class-name "min-w-10 h-10 object-cover rounded-sm"})
                              ($ Image {:class-name "w-10 h-10 scale-[1.2]"})))

                         ($ TableCell {:class-name ""} (str (:product field) " " (:version field)))

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
