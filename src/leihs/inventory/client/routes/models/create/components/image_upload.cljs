(ns leihs.inventory.client.routes.models.create.components.image-upload
  (:require
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
  (let [[files set-files!] (uix.core/use-state nil)
        [error set-error!] (uix.core/use-state nil)
        handle-drop (fn [files rejections event]
                      (if (seq rejections)
                        (set-error! (str "Error Uploading Files"))
                        (set-files!
                         (fn [prev]
                           (vec (concat prev files))))))

        handle-delete (fn [index]
                        (set-files!
                         (fn [prev]
                           (vec (concat (subvec prev 0 index)
                                        (subvec prev (inc index)))))))

        handle-cover (fn [index]
                       (set-files!
                        (fn [prev]
                          (vec (map-indexed
                                (fn [i file]
                                  (if (= i index)
                                    (do (aset file "isCover" true) file)
                                    (do (aset file "isCover" false) file)))
                                prev)))))]

    ($ RadioGroup {:defaultValue nil
                   :onValueChange #(handle-cover %)}

       ($ FormField
          {:control (cj control)
           :name "images"
           :render #($ FormItem {:class-name "mt-6"}
                       ($ FormLabel "Upload Image")
                       ($ FormControl
                          ($ Dropzone
                             ($ DropzoneArea (merge
                                              {:multiple (:multiple props)
                                               :filetypes (:filetypes props)
                                               :onDrop (fn [files rej ev] (handle-drop files rej ev))}
                                              (:field (jc %))))

                             (when error ($ :span {:className "text-xs text-red-600 mt-3"} error))

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
                                                   ($ RadioGroupItem {:value index}))
                                                ($ TableCell
                                                   ($ :div {:className "flex justify-end"}
                                                      ($ Button {:variant "outline"
                                                                 :size "icon"
                                                                 :type "button"
                                                                 :onClick (fn [] (handle-delete index))
                                                                 :className "select-none cursor-pointer"}

                                                         ($ Trash {:className "w-4 h-4"})))))))))))))

                       ($ FormMessage))}))))

(def ImageUpload
  (uix/as-react
   (fn [props]
     (main props))))
