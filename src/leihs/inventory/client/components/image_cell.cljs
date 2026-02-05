(ns leihs.inventory.client.components.image-cell
  (:require
   ["@@/button" :refer [Button]]
   ["@@/dialog" :refer [Dialog DialogContent DialogHeader DialogTitle
                        DialogTrigger]]
   ["@@/table" :refer [TableCell]]
   ["lucide-react" :refer [Image]]
   [uix.core :as uix :refer [$ defui]]))

(defui ImageCell [{:keys [field]}]
  ($ TableCell {:class-name "w-0"}
     (if (:url field)
       ($ Dialog
          ($ DialogTrigger {:as-child true}
             ($ Button {:variant "outline"
                        :data-test-id (str (:product field) "-preview")
                        :class-name "p-0 w-10 h-10 hover:bg-white shadow-none align-middle"}
                ($ :img {:src (str (:url field) "/thumbnail")
                         :class-name "w-10 h-10 p-1 object-contain rounded"})))
          ($ DialogContent
             ($ DialogHeader
                ($ DialogTitle (:name field)))
             ($ :img {:src (:url field)
                      :class-name "w-[50vh] aspect-square object-contain"})))
       ($ Image {:class-name "w-10 h-10 scale-[1.2] align-middle"}))))
