(ns leihs.inventory.client.routes.models.crud.page
  (:require
   ["@/components/react/scrollspy/scrollspy" :refer [Scrollspy ScrollspyItem
                                                     ScrollspyMenu]]
   ["@/routes/models/components/form" :refer [schema structure]]
   ["@@/alert-dialog" :refer [AlertDialog AlertDialogContent AlertDialogFooter
                              AlertDialogAction AlertDialogHeader
                              AlertDialogTitle AlertDialogDescription
                              AlertDialogCancel]]
   ["@@/button" :refer [Button]]
   ["@@/card" :refer [Card CardContent]]
   ["@@/form" :refer [Form]]
   ["@hookform/resolvers/zod" :refer [zodResolver]]
   ["lucide-react" :refer [Trash]]
   ["react-hook-form" :refer [useForm]]
   ["react-router-dom" :as router :refer [Link useLoaderData]]
   ["sonner" :refer [toast]]
   [cljs.core.async :as async :refer [go]]
   [clojure.string :as str]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [leihs.inventory.client.routes.models.components.forms.fields :as form-fields]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

;; (defn on-submit [data event]
;;   (go
;;     (let [form-data (js/FormData.)]
;;       (.. event (preventDefault))
;;       (js/console.debug "is valid " data)
;;
;;       (doseq [[k v] (js/Object.entries data)]
;;         (cond
;;         ;; add images as binary data
;;           (= k "images")
;;           (if (js/Array.isArray v)
;;             (doseq [val v]
;;               (.. form-data (append (str "images") val)))
;;             (.. form-data (append "images" v)))
;;
;;         ;; add attachments as binary data
;;           (= k "attachments")
;;           (if (js/Array.isArray v)
;;             (doseq [val v]
;;               (.. form-data (append "attachments" val)))
;;             (.. form-data (append "attachments" v)))
;;
;;         ;; add fields as text data
;;           :else (let [value (js/JSON.stringify v)]
;;                   (.. form-data (append k value)))))
;;
;;       #_(js/fetch "http://localhost:5002/api/sample"
;;                   (cj {:method "POST"
;;                        :body form-data}))
;;
;;       (.. (js/fetch "/inventory/8bd16d45-056d-5590-bc7f-12849f034351/model"
;;                     (cj {:method "POST"
;;                          :headers {"Accept" "application/json"}
;;                          :body form-data}))
;;           (then (js/console.debug "success"))
;;           (catch (fn [err] (js/console.debug "error" err)))))))

(defn- on-invalid [data]
  (js/console.debug "is invalid: " data))

(def default-values {:product ""
                     :is_package false
                     :manufacturer ""
                     :description ""
                     :internal_description ""
                     :technical_detail ""
                     :hand_over_note ""
                     :version ""
                     :image_attributes []
                     :categories []
                     :entitlements []
                     :properties []
                     :accessories []})

(defui page []
  (let [location (router/useLocation)
        is-create (str/includes? (:pathname (jc location)) "create")
        is-delete (str/includes? (:pathname (jc location)) "delete")
        model (into {} (:model (jc (router/useLoaderData))))

        form (useForm (cj {:resolver (zodResolver schema)
                           :defaultValues (if is-create default-values model)}))

        params (router/useParams)
        handleSubmit (:handleSubmit (jc form))
        control (:control (jc form))
        on-submit (fn [data event]
                    (go
                      (let [form-data (js/FormData.)]

                        ;; (.. form ())

                        (.. event (preventDefault))
                        (js/console.debug "is valid " data)
                        (.. toast (error "ERROR"))

                        (doseq [[k v] (js/Object.entries data)]
                          (cond
                          ;; add images as binary data
                            (= k "images")
                            (if (js/Array.isArray v)
                              (doseq [val v]
                                (.. form-data (append (str "images") val)))
                              (.. form-data (append "images" v)))

                            ;; add attachments as binary data
                            (= k "attachments")
                            (if (js/Array.isArray v)
                              (doseq [val v]
                                (.. form-data (append "attachments" val)))
                              (.. form-data (append "attachments" v)))

                          ;; add fields as text data
                            :else (let [value (js/JSON.stringify v)]
                                    (.. form-data (append k value)))))

                        (js/console.debug "data" (js/JSON.stringify data))

                        #_(if is-create

                            (.. (js/fetch (router/generatePath "/inventory/:pool-id" params)
                                          (cj {:method "POST"
                                               :headers {"Accept" "application/json"}
                                               :body form-data}))
                                (then (js/console.debug "success"))
                                (catch (fn [err] (js/console.debug "error" err))))

                            (.. (js/fetch (router/generatePath "/inventory/:pool-id/model/:model-id" params)
                                          (cj {:method "PUT"
                                               :headers {"Accept" "application/json"}
                                               :body form-data}))
                                (then (js/console.debug "success"))
                                (catch (fn [err] (js/console.debug "error" err))))))))]

;; (uix/use-effect
    ;;  (fn []
    ;;    (when (and (not is-create) model)
    ;;      (js/console.debug "set model values" (into {} model))))
    ;;
    ;;  [is-create model])

    ;; without this, form data is stale.
    ;; But this also means the form is evaluated every render
    ;; (.. form (watch))

    ($ :article
       ($ :h1 {:className "text-2xl bold font-bold mt-12 mb-6"}
          "Inventarliste - Ausleihe Toni Areal")

       ($ :h3 {:className "text-sm mt-12 mb-6 text-gray-500"}
          "Nehmen Sie Änderungen vor und speichern Sie anschliessend")

       ($ Card {:className "py-8 mb-12"}
          ($ CardContent
             ($ Scrollspy {:className "flex gap-4"}
                ($ ScrollspyMenu)

                ($ Form (merge form)
                   ($ :form {:id "create-model"
                             :className "space-y-12 w-3/5"
                             :on-submit (handleSubmit on-submit on-invalid)}

                      (for [section (jc structure)]
                        ($ ScrollspyItem {:className "scroll-mt-[10vh]"
                                          :key (:title section)
                                          :id (:title section)
                                          :name (:title section)}

                           ($ :h2 {:className "text-lg"} (:title section))
                           ($ :hr {:className "mb-4"})

                           (for [block (:blocks section)]
                             ($ form-fields/field {:key (:name block)
                                                   :control control
                                                   :form form
                                                   :block block}))))))

                ($ :div {:className "h-max flex space-x-6 sticky top-[43vh] ml-auto"}

                   ($ Link {:to (router/generatePath "/inventory/:pool-id/models" params)
                            :className "self-center hover:underline"}
                      "Abbrechen")

                   ($ Button {:type "submit"
                              :form "create-model"
                              :className "self-center"}
                      (if is-create "Erstellen" "Speichern"))

                   (when (not is-create)
                     ($ Button {:asChild true
                                :variant "destructive"
                                :size "icon"
                                :className "self-center"}
                        ($ Link {:to (router/generatePath "/inventory/:pool-id/models/:model-id/delete" params)}
                           ($ Trash {:className "w-4 h-4"}))))

                   ;; Dialog when deleting a model
                   (when (not is-create)
                     ($ AlertDialog {:open is-delete}
                        ($ AlertDialogContent

                           ($ AlertDialogHeader
                              ($ AlertDialogTitle "Modell löschen")
                              ($ AlertDialogDescription "Sind Sie sicher, dass Sie dieses Modell löschen möchten?"))

                           ($ AlertDialogFooter
                              ($ AlertDialogAction
                                 ($ Link {:to (router/generatePath "/inventory/:pool-id/models" params)}
                                    "Löschen"))
                              ($ AlertDialogCancel
                                 ($ Link {:to (router/generatePath "/inventory/:pool-id/models/:model-id" params)}
                                    "Abbrechen")))))))))))))


