(ns leihs.inventory.client.routes.models.create.page
  (:require
   ["@/components/react/scrollspy/scrollspy" :refer [Scrollspy ScrollspyItem
                                                     ScrollspyMenu]]
   ["@/routes/models/create/form" :refer [schema structure]]
   ["@@/button" :refer [Button]]
   ["@@/card" :refer [Card CardContent]]
   ["@@/form" :refer [Form]]
   ["@hookform/resolvers/zod" :refer [zodResolver]]
   ["@tanstack/react-query" :as react-query :refer [useQuery]]
   ["react-hook-form" :refer [useForm]]
   ["react-router-dom" :as router :refer [Link]]
   [cljs.core.async :as async :refer [<! go]]
   [cljs.core.async.interop :refer-macros [<p!]]
   [clojure.string :as str]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [leihs.inventory.client.routes.models.create.context :refer [state-context]]
   [leihs.inventory.client.routes.models.create.fields :as form-fields]
   [shadow.cljs.modern :refer (js-await)]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

;; +        async function computeSHA256(file) {
;; +            const buffer = await file.arrayBuffer(); // Read file as ArrayBuffer
;; +            const hashBuffer = await crypto.subtle.digest("SHA-256", buffer); // Generate SHA-256
;; +            return Array.from(new Uint8Array(hashBuffer)) // Convert buffer to hex string
;; +                .map(b => b.toString(16).padStart(2, "0"))
;; +                .join("");
;; +        }

;; (defn my-async-fn [foo]
;;   (-> (promise-producing-call foo)
;;       (.then (fn [the-result]
;;                (do-something-with-the-result the-result)))
;;       (.catch (fn [failure]
                ;; (prn [:oh-oh failure])))))

(defn get-buffer [file]
  (-> (.. file (arrayBuffer))
      (.then (fn [buffer] buffer))
      (.catch (fn [err] (js/console.error err)))))

(defn get-hash-buffer [buffer]
  (-> (.digest (.-subtle js/crypto) "SHA-256" buffer)
      (.then (fn [hash-buffer] hash-buffer))
      (.catch (fn [err] (js/console.error err)))))

(defn compute-sha256-hash [file]
  (let [buffer (get-buffer file) ; Read file as ArrayBuffer
        hash-buffer (get-hash-buffer buffer) ; Generate SHA-256
        hash-array (js/Uint8Array. hash-buffer)
        hash-hex (map #(-> %
                           (.. (toString 16))
                           (.. (padStart 2 "0")))
                      (js/Array.from hash-array))
        hash (str/join "" hash-hex)]

    (js/console.debug "hash" buffer)

    (js/Promise.
     (fn [resolve _reject]
       (resolve hash)))))

(defn on-submit [data event]
  (let [form-data (js/FormData.)]
    (.. event (preventDefault))
    (js/console.debug "is valid " data)

    (doseq [[k v] (js/Object.entries data)]
      (cond
        ;; add images as binary data
        (= k "images")
        (if (js/Array.isArray v)
          (doseq [val v]
            (js-await [hash (compute-sha256-hash val)]
                      (js/console.debug "hash-bla" hash)
                      (.. form-data (append (str "images") val))))

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

    #_(js/fetch "http://localhost:5002/api/sample"
                (cj {:method "POST"
                     :body form-data}))

    (js/fetch "/inventory/8bd16d45-056d-5590-bc7f-12849f034351/model"
              (cj {:method "POST"
                   :headers {"Accept" "application/json"}
                   :body form-data}))))

(defn- on-invalid [data]
  (js/console.debug "is invalid: " data))

(defn fetch-entitlement-groups [params]
  (let [path (router/generatePath "/inventory/:pool-id/entitlement-groups" params)]
    (.. (js/fetch path
                  (cj {:headers {"Accept" "application/json"}}))
        (then #(.json %))
        (then #(jc %)))))

(defn fetch-categories []
  (.. (js/fetch "/inventory/tree"
                (cj {:headers {"Accept" "application/json"}}))
      (then #(.json %))
      (then #(jc %))))

(defn fetch-models []
  (.. (js/fetch "/inventory/models-compatibles"
                (cj {:headers {"Accept" "application/json"}}))
      (then #(.json %))
      (then #(jc %))))

(defn fetch-manufacturers []
  (.. (js/fetch "/inventory/manufacturers?type=Model"
                (cj {:headers {"Accept" "application/json"}}))
      (then #(.json %))
      (then #(jc %))))

(defui page []
  (let [form (useForm (cj {:resolver (zodResolver schema)
                           :defaultValues {:product ""
                                           :isPackage false
                                           :manufacturer ""
                                           :description ""
                                           :internalDescription ""
                                           :technicalDetails ""
                                           :handOverNote ""
                                           :version ""
                                           :categories []
                                           :entitlements []
                                           :properties []
                                           :accessories []}}))
        params (router/useParams)
        handleSubmit (:handleSubmit (jc form))
        control (:control (jc form))
        entitlement-groups (jc (useQuery (cj {:queryKey ["entitlement-groups"]
                                              :queryFn #(fetch-entitlement-groups params)})))

        models (jc (useQuery (cj {:queryKey ["models"]
                                  :queryFn fetch-models})))

        manufacturers (jc (useQuery (cj {:queryKey ["manufacturers"]
                                         :queryFn fetch-manufacturers})))

        categories (jc (useQuery (cj {:queryKey ["categorories"]
                                      :queryFn fetch-categories})))]

    ;; without this, form data is stale.
    ;; But this also means the form is evaluated every render
    (.. form (watch))

    (cond
      (and (:isLoading entitlement-groups) (:isLoading categories))
      ($ :div "Loading...")

      (or (:isError entitlement-groups) (:isError categories))
      ($ :div "Error!")

      (and (:isSuccess entitlement-groups) (:isSuccess categories))
      ($ (.-Provider state-context) {:value {:models (:data models)
                                             :entitlements (:data entitlement-groups)
                                             :manufacturers (:data manufacturers)
                                             :categories (:data categories)}}
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
                           "Submit"))))))))))


