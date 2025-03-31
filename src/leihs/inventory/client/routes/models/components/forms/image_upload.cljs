(ns leihs.inventory.client.routes.models.components.forms.image-upload
  (:require
   ["@@/button" :refer [Button]]
   ["@@/dropzone" :refer [Dropzone DropzoneArea DropzoneFiles ErrorMessages
                          Item]]
   ["@@/radio-group" :refer [RadioGroup RadioGroupItem]]
   ["@@/table" :refer [Table TableBody TableCell TableHead TableHeader
                       TableRow]]
   ["lucide-react" :refer [Trash]]
   [leihs.inventory.client.lib.utils :refer [cj]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defn delete-by-index [index vector]
  (vec (concat (subvec vector 0 index)
               (subvec vector (inc index)))))

(defui main [{:keys [control form props]}]
  (let [[images set-images!] (uix.core/use-state [])
        [error set-error!] (uix.core/use-state nil)
        [cover-index set-cover-index!] (uix.core/use-state nil)

        set-value (aget form "setValue")
        handle-drop (fn [files rejections _event]
                      (if (seq rejections)
                        (set-error! rejections)
                        (set-error! nil))

                      (set-images!
                       (fn [prev]
                         (vec (concat prev
                                      (map (fn [file]
                                             {:file file
                                              :is_cover false})
                                           files))))))

        handle-delete (fn [index]
                        ;; remove file by index on delete
                        (set-images!
                         (fn [prev] (delete-by-index index prev))))

        handle-cover (fn [index]
                       (set-cover-index! index)
                       (set-images! #(vec (map-indexed
                                           (fn [i attrs]
                                             (if (= i index)
                                               (assoc attrs :is_cover true)
                                               (assoc attrs :is_cover false)))
                                           %))))]

    (uix/use-effect
     (fn []
       (set-value "images" (cj (vec images)))
       [set-value images]))

    ($ RadioGroup {:value cover-index
                   :onValueChange handle-cover}

       ($ Dropzone
          ($ DropzoneArea (merge
                           {:multiple (:multiple props)
                            :filetypes (:filetypes props)
                            :onDrop handle-drop}))

          (when error ($ ErrorMessages {:rejections error}))

          (when (seq images)
            ($ DropzoneFiles
               ($ Table
                  ($ TableHeader
                     ($ TableRow
                        ($ TableHead "Bezeichnung")
                        ($ TableHead "Coverbild")
                        ($ TableHead "")))
                  ($ TableBody
                     (for [[index item] (map-indexed vector images)]
                       ($ TableRow {:key (.. (:file item) -name)}

                          ($ Item {:file (:file item)}
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
