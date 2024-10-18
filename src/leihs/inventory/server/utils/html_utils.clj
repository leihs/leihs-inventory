(ns leihs.inventory.server.utils.html-utils
  (:require [hickory.core :as h]
   [hickory.select :as s]
   [hickory.render :as render]
   [clojure.string :as str]
   [clojure.walk :as walk]))

(defn add-csrf-tags
  [html-str {:keys [authFlow csrfToken]}]
  (try
    (println ">o> abc1" html-str)
    (let [parsed-html (h/parse html-str)
          hickory-tree (h/as-hickory parsed-html)
          csrf-name (:name csrfToken)
          csrf-value (:value csrfToken)

          p (println ">o> authFlow / csrfToken" authFlow csrfToken (type csrfToken))
          p (println ">o> csrf-name / csrf-value" csrf-name csrf-value (type csrf-value))


          add-meta-tag (fn [tree]
                         (update-in tree [:content]
                           (fn [content]
                             (let [


                               head-index (first (keep-indexed #(when (= (:tag %2) :head) %1) content))
                                   p (println ">o> head-index2" head-index)

                                   head (nth content head-index)
                                   p (println ">o> head" head)
                                   updated-head (update head :content conj {:type :element
                                                                            :tag :meta
                                                                            :attrs {:name csrf-name :content csrf-value}})

                                   p (println ">o> updated-head" updated-head)

                                   ]
                               (assoc content head-index updated-head)))))

          update-csrf-input (fn [tree]
                              (walk/postwalk
                                (fn [node]
                                  (if (and (map? node)
                                        (= (:tag node) :input)
                                        (= (get-in node [:attrs :name]) "csrfToken")
                                        (= (get-in node [:attrs :type]) "hidden"))
                                    (assoc-in node [:attrs :value] csrf-value)
                                    node))
                                tree))

          updated-tree (-> hickory-tree
                         add-meta-tag
                         update-csrf-input)]

      (println ">o> abc2")
      (str "<!DOCTYPE html>\n" (render/hickory-to-html updated-tree)))
    (catch Exception e
      (println "Error in add-csrf-tags:" (.getMessage e))
      (.printStackTrace e)
      html-str)))  ; Return original HTML in case of error













(defn add-csrf-tags
  [html-str {:keys [authFlow csrfToken]}]
  (try
    (println ">o> abc1" html-str)

    ;; Parse the HTML into a Hickory tree
    (let [parsed-html (h/parse html-str)
          hickory-tree (h/as-hickory parsed-html)
          csrf-name (:name csrfToken)
          csrf-value (:value csrfToken)]

      ;; Log the authFlow and csrfToken for debugging
      (println ">o> authFlow / csrfToken" authFlow csrfToken (type csrfToken))
      (println ">o> csrf-name / csrf-value" csrf-name csrf-value (type csrf-value))

      ;; Function to add the CSRF meta tag into the <head>
      (defn add-meta-tag [tree]
        (walk/postwalk
          (fn [node]
            (if (and (map? node) (= (:tag node) :head))
              ;; Add the CSRF meta tag to the <head> content
              (update node :content conj {:type :element
                                          :tag :meta
                                          :attrs {:name csrf-name :content csrf-value}})
              node))
          tree))

      ;; Function to update the CSRF token in the hidden input field
      (defn update-csrf-input [tree]
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

      ;; Apply the transformations: add the meta tag and update the hidden input field
      (let [updated-tree (-> hickory-tree
                           add-meta-tag
                           update-csrf-input)]

        ;; Convert the updated Hickory tree back to HTML
        (println ">o> abc2")
        (str "<!DOCTYPE html>\n" (render/hickory-to-html updated-tree))))

    (catch Exception e
      (println "Error in add-csrf-tags:" (.getMessage e))
      (.printStackTrace e)
      html-str)))
