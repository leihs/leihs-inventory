(ns leihs.inventory.client.routes.models.components.forms.image-upload
  (:require
   ["@@/button" :refer [Button]]
   ["@@/dropzone" :refer [Dropzone DropzoneArea DropzoneFiles Item]]
   ["@@/radio-group" :refer [RadioGroup RadioGroupItem]]
   ["@@/table" :refer [Table TableBody TableCell TableHead TableHeader
                       TableRow]]
   ["lucide-react" :refer [Trash]]
   [cljs.core.async :as async :refer [go <!]]
   [cljs.core.async.interop :refer-macros [<p!]]
   [clojure.string :as str]
   [leihs.inventory.client.lib.utils :refer [cj]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defn compute-sha256 [file]
  (go
    (let [buffer (<p! (.arrayBuffer file)) ; Read file as ArrayBuffer
          hash-buffer (<p! (.digest (.-subtle js/crypto) "SHA-256" buffer)) ; Generate SHA-256
          hash-array (js/Uint8Array. hash-buffer)
          hash-hex (map #(-> %
                             (.. (toString 16))
                             (.. (padStart 2 "0")))
                        (js/Array.from hash-array))
          hash (str/join "" hash-hex)]
      hash)))

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
       (go
         (when (seq files)
           (doseq [file files]
             (let [hash (<! (compute-sha256 file))]

               #_(js/console.log "hash" hash)))))

       ;; (let [imgattr (map (fn [el] (go (hash-map :is-cover false
       ;;                                           :checksum (<! (compute-sha256 el))
       ;;                                           :is-delete false)))
       ;;                    (vec files))]
       ;;   (js/console.log imgattr))

       (set-value "images" (cj (vec files)))
       ;; (set-value "image-attributes" (cj (map #(hash-map :is-cover false
       ;;                                                   :checksum (<! (compute-sha256 %))
       ;;                                                   :is-delete false)
       ;;                                        (vec files))))
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
