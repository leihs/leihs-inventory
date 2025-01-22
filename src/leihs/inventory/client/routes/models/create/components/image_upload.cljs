(ns leihs.inventory.client.routes.models.create.components.image-upload
  (:require
   ["@/components/react/sortable-list" :refer [SortableList Draggable DragHandle]]
   ["@@/dropzone" :refer [Dropzone Item DropzoneArea DropzoneFiles]]
   ["@@/form" :refer [FormField FormLabel FormItem FormControl FormMessage]]
   ["@@/radio-group" :refer [RadioGroup, RadioGroupItem]]
   ["react-dropzone" :refer [useDropzone]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [uix.core :as uix :refer [defui $]]
   [uix.dom]))

(defui main [{:keys [control props]}]
  (let [[coverIndex setCoverIndex!] (uix.core/use-state "")
        [files setFiles!] (uix.core/use-state nil)]

    ($ RadioGroup {:defaultValue nil
                   :onValueChange #(setCoverIndex! %)}

       ($ FormField {:control (cj control)
                     :name "images"
                     :render #($ FormItem {:class-name "mt-6"}
                                 ($ FormLabel "Upload Image")
                                 ($ FormControl
                                    ($ Dropzone
                                       ($ DropzoneArea (merge
                                                        {:multiple true
                                                         :sortable false}
                                                        (:field (jc %))))
                                       ($ DropzoneFiles)))

                                 ($ FormMessage))}))))

(def ImageUpload
  (uix/as-react
   (fn [props]
     (main props))))
