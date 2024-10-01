(ns leihs.inventory.client.routes.models.create.components.image-upload
  (:require
   ["@@/dropzone" :refer [Dropzone]]
   ["lucide-react" :as lucide :refer [Upload]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defui main [{:keys [props field]}]
  ($ Dropzone (merge field)))


