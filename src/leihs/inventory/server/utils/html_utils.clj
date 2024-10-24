(ns leihs.inventory.server.utils.html-utils
  (:require [hickory.core :as h]
   [hickory.select :as s]
   [hickory.render :as render]
   [clojure.string :as str]
   [clojure.walk :as walk]

   [clojure.pprint :as pp]
   ))

(defn add-meta-tag [tree csrf-name csrf-value]

  (println ">o> add-meta-tag" tree csrf-name csrf-value)

  (walk/postwalk
    (fn [node]
      (if (and (map? node) (= (:tag node) :head))
        ;; Add the CSRF meta tag to the <head> content
        (update node :content conj {:type :element
                                    :tag :meta
                                    :attrs {:name csrf-name :content csrf-value}})
        node))
    tree))

(defn update-csrf-input [tree csrf-value]
  (walk/postwalk
    (fn [node]
      (if (and (map? node)
            (= (:tag node) :input)
            (= (get-in node [:attrs :name]) "csrfToken")
            (= (get-in node [:attrs :type]) "hidden"))
        ;; Update the value of the hidden input field for the CSRF token
        (assoc-in node [:attrs :value] csrf-value)
        node))
    tree))

(defn add-form-if-missing [tree csrf-name csrf-value]
  (walk/postwalk
    (fn [node]
      (if (and (map? node) (= (:tag node) :body))
        ;; Check if there's a form inside the <body>
        (if (empty? (filter #(= (:tag %) :form) (:content node)))
          ;; No form exists, so add one with a hidden CSRF input field
          (update node :content conj {:type :element
                                      :tag :form
                                      :attrs {:name csrf-name}
                                      :content [{:type :element
                                                 :tag :input
                                                 :attrs {:type "hidden" :name csrf-name :value csrf-value}}]})
          node)
        node))
    tree))

(defn add-return-field [tree return-field]
   (println ">o> tree" tree)
  (println ">o> return-field" return-field)
  (walk/postwalk
    (fn [node]
      (if (and (map? node)
            (= (:tag node) :form))
        ;; Add the return-to input field if it doesn't already exist
        (if (not (some #(and (map? %) (= (:tag %) :input) (= (get-in % [:attrs :name]) "return-to")) (:content node)))
          (update node :content conj return-field)
          node)
        node))
    tree))

(defn add-error-message-field [tree error-message-field]
   (println ">o> tree" tree)
  (println ">o> error-message-field" error-message-field)
  (walk/postwalk
    (fn [node]
      (if (and (map? node)
            (= (:tag node) :form))
        ;; Add the return-to input field if it doesn't already exist
        (if (not (some #(and (map? %) (= (:tag %) :input) (= (get-in % [:attrs :class]) "message")) (:content node)))
        ;(if (not (some #(and (map? %) (= (:tag %) :input) (= (get-in % [:attrs :name]) "message")) (:content node)))
          (update node :content conj error-message-field)
          node)
        node))
    tree))

(defn pprint-html [html-str]
  (with-out-str (pp/pprint html-str)))

(defn add-csrf-tags
  [html-str {:keys [authFlow csrfToken]}]
  (try
    (let [
          p (println ">o> html-str" html-str)

          parsed-html (h/parse html-str)
          p (println ">o> parsed-html" parsed-html)

          hickory-tree (h/as-hickory parsed-html)
          p (println ">o> hickory-tree" hickory-tree)

          ;parsed-html (h/parse html-str)
          ;hickory-tree (h/as-hickory parsed-html)


          p (println ">o> returnTo??????" (:returnTo authFlow) "<correct??")

          returnTo (:returnTo authFlow)

          csrf-name (:name csrfToken)
          csrf-value (:value csrfToken)
          return-field {:type :element
                        :tag :input
                        :attrs {:type "hidden" :name "return-to" :value returnTo}}
          ;]

      ;; Log the authFlow and csrfToken for debugging
      _ (println ">o> authFlow / csrfToken" authFlow csrfToken (type csrfToken))
      _ (println ">o> csrf-name / csrf-value" csrf-name csrf-value (type csrf-value))

      ;; Apply the transformations: add the meta tag, update the hidden input field, add return field, and add form if missing
      ;(let [
            ;updated-tree (-> hickory-tree
            ;               (add-meta-tag csrf-name csrf-value)
            ;               (update-csrf-input csrf-value)
            ;               (add-return-field return-field)
            ;               (add-form-if-missing csrf-name csrf-value))

            updated-tree (-> hickory-tree
                                 (add-meta-tag  csrf-name csrf-value)
                                 (update-csrf-input  csrf-value)
                                 ;(add-return-field hickory-tree return-field)
                                 (add-form-if-missing  csrf-name csrf-value)
                           )

            raw-html (render/hickory-to-html updated-tree)
            ;raw-html (pprint-html raw-html)
            ]
        raw-html)

    (catch Exception e
      (println "Error in add-csrf-and-return-tags:" (.getMessage e))
      (.printStackTrace e)
      html-str)))


(defn add-or-create-return-to-tag
  [html-str {:keys [authFlow csrfToken]}]
  (try
    ;(println ">o> abc1" html-str)

    ;; Parse the HTML into a Hickory tree
    (let [
          p (println ">o> html-str" html-str)

          parsed-html (h/parse html-str)
          p (println ">o> parsed-html" parsed-html)

          hickory-tree (h/as-hickory parsed-html)
          p (println ">o> hickory-tree" hickory-tree)

          csrf-name (:name csrfToken)
          csrf-value (:value csrfToken)
          returnTo (:returnTo authFlow)

          return-field {:type :element
                        :tag :input
                        :attrs {:type "hidden" :name "return-to" :value returnTo}}

          ;]

      ;; Log the authFlow and csrfToken for debugging
      _ (println ">o> authFlow / csrfToken" authFlow csrfToken (type csrfToken))
      _ (println ">o> csrf-name / csrf-value" csrf-name csrf-value (type csrf-value))

      ;;; Function to add t

      ;; Apply the transformations: add the meta tag, update the hidden input field, and add form if missing
      ;(let [
            ;updated-tree (-> hickory-tree
            ;               ;add-meta-tag
            ;               ;update-csrf-input
            ;               ;add-form-if-missing
            ;               (add-return-field return-to)     ;; arity issue
            ;               )


            updated-tree (add-return-field hickory-tree return-field)


        ;; Convert the updated Hickory tree back to HTML
        _ (println ">o> abc2")
        ;(str "<!DOCTYPE html>\n" (render/hickory-to-html updated-tree))))
        html (render/hickory-to-html updated-tree)
            ;html   (pprint-html  html)
            ]

        html
        )

    (catch Exception e
      (println "Error in add-csrf-tags:" (.getMessage e))
      (.printStackTrace e)
      html-str)))  ;; Return original HTML in case of error


(defn add-or-create-error-tag
  [html-str {:keys [authFlow csrfToken]}]
  (try
    ;(println ">o> abc1" html-str)

    ;; Parse the HTML into a Hickory tree
    (let [
          p (println ">o> html-str" html-str)

          parsed-html (h/parse html-str)
          p (println ">o> parsed-html" parsed-html)

          hickory-tree (h/as-hickory parsed-html)
          p (println ">o> hickory-tree" hickory-tree)

          csrf-name (:name csrfToken)
          csrf-value (:value csrfToken)
          returnTo (:returnTo authFlow)
          errorMessage (:errorMessage authFlow)

          errorMessageField {:type :element
                        :tag :div
                        :attrs {:class "message" :value errorMessage}}

          ;]

      ;; Log the authFlow and csrfToken for debugging
      _ (println ">o> authFlow / csrfToken" authFlow csrfToken (type csrfToken))
      _ (println ">o> csrf-name / csrf-value" csrf-name csrf-value (type csrf-value))

      ;;; Function to add t

      ;; Apply the transformations: add the meta tag, update the hidden input field, and add form if missing
      ;(let [
            ;updated-tree (-> hickory-tree
            ;               ;add-meta-tag
            ;               ;update-csrf-input
            ;               ;add-form-if-missing
            ;               (add-return-field return-to)     ;; arity issue
            ;               )


            updated-tree (add-error-message-field hickory-tree errorMessageField)


        ;; Convert the updated Hickory tree back to HTML
        _ (println ">o> abc2")
        ;(str "<!DOCTYPE html>\n" (render/hickory-to-html updated-tree))))
        html (render/hickory-to-html updated-tree)
            ;html   (pprint-html  html)
            ]

        html
        )

    (catch Exception e
      (println "Error in add-csrf-tags:" (.getMessage e))
      (.printStackTrace e)
      html-str)))  ;; Return original HTML in case of error









;;; TODO: remove this to test csrfToken
;(defn add-csrf-tags  [html-str {:keys [authFlow csrfToken]}]
;  html-str)


;; TODO: works
(defn add-csrf-tags2
  [html-str {:keys [authFlow csrfToken]}]
  (let [csrf-name (:name csrfToken)
        csrf-value (:value csrfToken)
        meta-tag (str "<meta name=\"" csrf-name "\" content=\"" csrf-value "\">")

        with-meta-tag (if (re-find #"<head>" html-str)
                        (clojure.string/replace-first html-str #"<head>" (str "<head>" meta-tag))
                        html-str)

        updated-html (clojure.string/replace with-meta-tag
                       #"<input name=\"csrfToken\" type=\"hidden\" value=\"[^\"]*\""
                       (str "<input name=\"csrfToken\" type=\"hidden\" value=\"" csrf-value "\""))
        ]
    updated-html))