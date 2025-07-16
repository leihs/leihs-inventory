(ns leihs.inventory.client.routes.models.crud.components.compatible-models
  (:require
   ["@/components/ui/command" :refer [Command CommandEmpty CommandInput
                                      CommandItem CommandList]]
   ["@/components/ui/popover" :refer [Popover PopoverContent PopoverTrigger]]
   ["@@/button" :refer [Button]]
   ["@@/table" :refer [Table TableBody TableCell TableRow]]
   ["lucide-react" :refer [Check ChevronsUpDown Image Trash]]
   ["react-hook-form" :as hook-form]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :refer [useLoaderData]]
   [clojure.string :as str]
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

(defui main [{:keys [control models form props]}]
  (let [{:keys [models]} (useLoaderData)
        [t] (useTranslation)
        [open set-open!] (uix/use-state false)
        [width set-width!] (uix/use-state nil)
        [search set-search!] (uix/use-state "")
        buttonRef (uix/use-ref nil)
        [filtered-models set-filtered-models!] (uix/use-state [])

        {:keys [fields append remove]} (jc (hook-form/useFieldArray
                                            (cj {:control control
                                                 :name "compatibles"})))]

    (uix/use-effect
     (fn []
       (let [filtered (filter #(str/starts-with?
                                (str/lower-case (:product %))
                                (str/lower-case search))
                              models)]
         (if (= search "")
           (set-filtered-models! [])
           (set-filtered-models! filtered))))
     [models search])

    (uix/use-effect
     (fn []
       (when (.. buttonRef -current)
         (set-width! (.. buttonRef -current -offsetWidth))))
     [])

    ($ :div {:class-name "flex flex-col gap-2"}
       ($ Popover {:open open
                   :on-open-change #(do
                                      (set-search! "")
                                      (set-filtered-models! [])
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

             ($ Command {:on-change #(set-search! (.. % -target -value))}
                ($ CommandInput {:placeholder (t "pool.model.compatible_models.blocks.compatible_models.search")})
                ($ CommandList {:data-test-id "compatible-models-list"}

                   ($ CommandEmpty (t "pool.model.compatible_models.blocks.compatible_models.not_found"))

                   (for [model filtered-models]
                     ($ CommandItem {:key (:model_id model)
                                     :value (str (:product model) " " (:version model))
                                     :on-select #(do (set-open! false)
                                                     (if
                                                      (not (check-path-existing (:product model) fields))
                                                       (append (cj {:product (:product model)
                                                                    :version (:version model)
                                                                    :id (:model_id model)
                                                                    :cover_image_id (:cover_image_id model)
                                                                    :cover_image_url (:cover_image_url model)}))
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
                           (str (:product model) " " (:version model)))))))))

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
