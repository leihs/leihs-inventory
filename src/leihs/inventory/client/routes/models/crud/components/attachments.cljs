(ns leihs.inventory.client.routes.models.crud.components.attachments
  (:require
   ["@@/button" :refer [Button]]
   ["@@/dropzone" :refer [Dropzone DropzoneArea DropzoneFiles ErrorMessages
                          Item]]
   ["@@/table" :refer [Table TableBody TableCell TableHead TableHeader
                       TableRow]]
   ["lucide-react" :refer [Trash Eye]]
   ["react-router-dom" :as router :refer [useLoaderData]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defn delete-by-index [index vector]
  (vec (concat (subvec vector 0 index)
               (subvec vector (inc index)))))

(defn get-url-from-id [id items]
  (when (seq items)
    (some #(when (= id (:id %)) (:url %)) items)))

(defui main [{:keys [control form props]}]
  (let [set-value (aget form "setValue")
        get-values (aget form "getValues")
        navigate (router/useNavigate)
        {:keys [model]} (useLoaderData)

        [attachments set-attachments!] (uix.core/use-state [])
        [error set-error!] (uix.core/use-state nil)

        handle-drop (fn [files rejections _event]
                      (if (seq rejections)
                        (set-error! rejections)
                        (set-error! nil))

                      (set-attachments!
                       (fn [prev]
                         (vec (concat prev
                                      (map (fn [file]
                                             {:id nil
                                              :file file})
                                           files))))))

        handle-delete (fn [index]
                        ;; remove file by index on delete
                        (set-attachments!
                         (fn [prev] (delete-by-index index prev))))]

    (uix/use-effect
     (fn []
       (let [att (jc (get-values "attachments"))]
         (when (seq att)
           (set-attachments! att))))
     [get-values])

    (uix/use-effect
     (fn []
       (set-value "attachments" (cj (vec attachments))))
     [set-value attachments])

    ($ Dropzone
       ($ DropzoneArea (merge
                        {:multiple (:multiple props)
                         ;; :filetypes (:filetypes props)
                         :onDrop handle-drop}))

       (when error ($ ErrorMessages {:rejections error}))

       (when (seq attachments)
         ($ DropzoneFiles
            ($ Table
               ($ TableHeader
                  ($ TableRow
                     ($ TableHead "Bezeichnung")
                     ($ TableHead "")))
               ($ TableBody
                  (for [[index attachment] (map-indexed vector attachments)]
                    ($ TableRow {:key (.. (:file attachment) -name)}

                       ($ Item {:file (:file attachment)
                                :generatePreview false}

                          ($ TableCell
                             ($ :div {:className "flex justify-end space-x-4"}
                                ($ Button {:asChild true
                                           :variant "outline"
                                           :size "icon"
                                           :type "button"
                                           :className "select-none cursor-pointer"}
                                   ($ :a {:target "_blank"
                                          :href (get-url-from-id
                                                 (:id attachment)
                                                 (:attachments model))}
                                      ($ Eye {:className "w-4 h-4"})))

                                ($ Button {:variant "outline"
                                           :size "icon"
                                           :type "button"
                                           :onClick (fn [] (handle-delete index))
                                           :className "select-none cursor-pointer"}

                                   ($ Trash {:className "w-4 h-4"}))))))))))))))

(def Attachments
  (uix/as-react
   (fn [props]
     (main props))))
