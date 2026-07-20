(ns leihs.inventory.client.components.image-modal
  (:require
   ["@@/dialog" :refer [Dialog DialogTrigger DialogContent
                        DialogHeader DialogTitle]]
   [uix.core :as uix :refer [$ defui]]))

(defui ImageModal [{:keys [url alt class-name]}]
  ($ Dialog
     ($ DialogTrigger {:asChild true}
        ($ :button {:type "button"}
           ($ :img {:class-name (str (when (not class-name) "min-w-12 h-12 ")
                                     "object-contain rounded border p-1 bg-white " class-name)
                    :src (str url "/thumbnail")
                    :loading "lazy"
                    :alt alt})))
     ($ DialogContent
        ($ DialogHeader {:class-name "mr-8"}
           ($ DialogTitle alt))
        ($ :img {:class-name "w-[50vh] aspect-square object-contain"
                 :src url
                 :loading "lazy"
                 :alt alt}))))

