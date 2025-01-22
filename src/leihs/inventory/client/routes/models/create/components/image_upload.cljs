(ns leihs.inventory.client.routes.models.create.components.image-upload
  (:require
   ["@@/dropzone" :refer [Dropzone]]
   ["@@/form" :refer [FormField FormLabel FormItem FormControl FormMessage]]
   ["@@/radio-group" :refer [RadioGroup, RadioGroupItem]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [uix.core :as uix :refer [defui $]]
   [uix.dom]))

(defui main [{:keys [control props]}]
  (let [[coverIndex setCoverIndex!] (uix.core/use-state "")
        [files setFiles!] (uix.core/use-state nil)]

    (uix.core/use-effect
     (fn []
       (js/console.debug "hello")
       (setFiles! (fn [prevFiles]
                    (doseq [[index file] (map-indexed vector prevFiles)]
                      (aset file "isCover" (= index coverIndex))))))
     [coverIndex])

    (defn handle-drop [files rejections setFiles]
      (setFiles! #(vec (concat % files)))
      (setFiles (fn [prevFiles]
                  (let [allFiles (cj (vec (concat prevFiles files)))]

                    (doseq [file allFiles]
                      (aset file "isCover" false))

                    allFiles))))

    ($ RadioGroup {:defaultValue 0 :onValueChange #(setCoverIndex! %)}
       ($ FormField {:control (cj control)
                     :name "images"
                     :render #($ FormItem {:class-name "mt-6"}
                                 ($ FormLabel "Upload Image")
                                 ($ FormControl
                                    ($ Dropzone (merge
                                                 {:multiple false
                                                  :itemExtensions (cj [{:head "Coverbild"
                                                                        :comp ($ RadioGroupItem)}])
                                                  :onDrop (fn [files rejections setFiles] (handle-drop files rejections setFiles))
                                                  :sortable true}
                                                 (:field (jc %)))))

                                 ($ FormMessage))}))))

(def ImageUpload
  (uix/as-react
   (fn [props]
     (main props))))
