(ns leihs.inventory.client.routes.models.components.forms.image-upload
  (:require
   ["@@/button" :refer [Button]]
   ["@@/dropzone" :refer [Dropzone DropzoneArea DropzoneFiles Item]]
   ["@@/radio-group" :refer [RadioGroup RadioGroupItem]]
   ["@@/table" :refer [Table TableBody TableCell TableHead TableHeader
                       TableRow]]
   ["lucide-react" :refer [Trash]]
   [leihs.inventory.client.lib.utils :refer [cj]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defui main [{:keys [control form props]}]
  (let [[files set-files!] (uix.core/use-state nil)
        [img-attributes set-img-attributes!] (uix.core/use-state nil)
        [error set-error!] (uix.core/use-state nil)
        set-value (aget form "setValue")
        handle-drop (fn [files rejections _event]
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

    (uix/use-effect
     (fn []
       (set-value "images" (cj (vec files)))
       [set-value files]))

    ($ RadioGroup {:defaultValue nil
                   :onValueChange #(handle-cover %)}

       ($ Dropzone
          ($ DropzoneArea (merge
                           {:multiple (:multiple props)
                            :filetypes (:filetypes props)
                            :onDrop handle-drop}))

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

                                      ($ Trash {:className "w-4 h-4"})))))))))))))))

(def ImageUpload
  (uix/as-react
   (fn [props]
     (main props))))
