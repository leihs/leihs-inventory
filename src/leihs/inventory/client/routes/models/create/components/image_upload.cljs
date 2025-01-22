(ns leihs.inventory.client.routes.models.create.components.image-upload
  (:require
   ["@/components/react/sortable-list" :refer [SortableList Draggable DragHandle]]
   ["@@/button" :refer [Button]]
   ["@@/dropzone" :refer [Dropzone Item DropzoneArea DropzoneFiles]]
   ["@@/form" :refer [FormField FormLabel FormItem FormControl FormMessage]]
   ["@@/radio-group" :refer [RadioGroup, RadioGroupItem]]
   ["@@/table" :refer [Table TableHeader TableRow TableHead TableBody TableCell]]
   ["lucide-react" :refer [Trash]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [uix.core :as uix :refer [defui $]]
   [uix.dom]))

(defui main [{:keys [control props]}]
  (let [[coverIndex setCoverIndex!] (uix.core/use-state "")
        [files setFiles!] (uix.core/use-state nil)
        handle-drop (fn [files rejections event]
                      (setFiles!
                       (fn [prev]
                         (vec (concat prev files)))))

        handle-delete (fn [index]
                        (setFiles!
                         (fn [prev]
                           (vec (concat (subvec prev 0 index)
                                        (subvec prev (inc index)))))))

        handle-cover (fn [index]
                       (setFiles!
                        (fn [prev]
                          (vec (map-indexed (fn [i file]
                                              (if (= i index)
                                                (aset file "isCover" true)))
                                            prev)))))]

    ($ RadioGroup {:defaultValue nil
                   :onValueChange #(handle-cover %)}

       ($ FormField {:control (cj control)
                     :name "images"
                     :render #($ FormItem {:class-name "mt-6"}
                                 ($ FormLabel "Upload Image")
                                 ($ FormControl
                                    ($ Dropzone
                                       ($ DropzoneArea (merge
                                                        {:multiple true
                                                         :onDrop (fn [files rej ev] (handle-drop files rej ev))}
                                                        (:field (jc %))))

                                       (when (seq files)
                                         ($ DropzoneFiles
                                            ($ Table
                                               ($ TableHeader
                                                  ($ TableRow
                                                     ($ TableHead "Bezeichnung")
                                                     ($ TableHead "Coverbild")
                                                     ($ TableHead "")))
                                               ($ TableBody
                                                  (for [[index file] (map-indexed vector files)]
                                                    ($ TableRow {:key (.. file -name)}

                                                       ($ Item {:file file}
                                                          ($ TableCell
                                                             ($ RadioGroupItem))
                                                          ($ TableCell
                                                             ($ :div {:className "flex justify-end"}
                                                                ($ Button {:variant "outline"
                                                                           :size "icon"
                                                                           :type "button"
                                                                           :onClick (fn [_] (handle-delete index))
                                                                           :className "select-none cursor-pointer"}

                                                                   ($ Trash {:className "w-4 h-4"})))))))))))))

                                 ($ FormMessage))}))))

(def ImageUpload
  (uix/as-react
   (fn [props]
     (main props))))
