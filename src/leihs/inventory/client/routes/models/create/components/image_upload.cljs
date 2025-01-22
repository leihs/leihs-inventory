(ns leihs.inventory.client.routes.models.create.components.image-upload
  (:require
   ["@@/dropzone" :refer [Dropzone]]
   ["@@/form" :refer [FormField FormLabel FormItem FormControl FormMessage]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [uix.core :as uix :refer [defui $]]
   [uix.dom]))

#_(defn handle-drop [files rejections setFiles]
    (setFiles (fn [prevFiles] (cj (vec (concat prevFiles files))))))

(defn handle-drop [files rejections setFiles]
  (setFiles (fn [prevFiles]
              (let [allFiles (cj (vec (concat prevFiles files)))]
                (doseq [file allFiles]
                  (aset file "isCover" false))
                allFiles))))

(defui main [{:keys [control props]}]
  ($ FormField {:control (cj control)
                :name "images"
                :render #($ FormItem {:class-name "mt-6"}
                            ($ FormLabel "Upload Image")
                            ($ FormControl
                               ($ Dropzone (merge
                                            {:multiple false
                                             :onDrop (fn [files rejections setFiles] (handle-drop files rejections setFiles))
                                             :sortable true}
                                            (:field (jc %)))))

                            ($ FormMessage))}))

(def ImageUpload
  (uix/as-react
   (fn [props]
     (main props))))
