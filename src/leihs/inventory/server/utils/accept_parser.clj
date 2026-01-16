(ns leihs.inventory.server.utils.accept-parser
  (:require [clojure.string :as str]))

(defn parse-accept-header
  "Parses Accept header into set of media types (without q-values).
   Returns {:types #{\"text/html\" \"image/webp\" ...} :has-wildcard? true/false}"
  [accept-header-str]
  (let [accept-str (or accept-header-str "")
        accept-str (if (str/blank? accept-str) "*/*" accept-str)
        types (->> (str/split (str/lower-case accept-str) #",")
                   (map #(first (str/split % #";")))
                   (map str/trim)
                   (remove str/blank?)
                   (into #{}))]
    {:types types
     :has-wildcard? (contains? types "*/*")}))

(defn accepts-type?
  "Check if parsed Accept header accepts given media type.
   Handles wildcards: */* matches anything, image/* matches image/png, etc."
  [{:keys [types has-wildcard?]} media-type]
  (or has-wildcard?
      (contains? types media-type)
      (some (fn [accepted-type]
              (when (str/ends-with? accepted-type "/*")
                (let [prefix (str/replace accepted-type #"/\*$" "")]
                  (str/starts-with? media-type (str prefix "/")))))
            types)))

(defn can-satisfy-any?
  "Check if route's :produces can satisfy ANY type in Accept header.
   route-produces: [\"application/json\" \"image/png\"]
   Returns true if route can produce at least one acceptable type."
  [parsed-accept route-produces]
  (if (empty? route-produces)
    true
    (some #(accepts-type? parsed-accept %) route-produces)))

(defn is-image-only-request?
  "True if Accept header contains ONLY image types (no wildcards, no html, no json).
   Examples:
   - 'image/png' → true
   - 'image/jpeg, image/webp' → true
   - 'image/webp, */*' → false (has wildcard)
   - 'text/html, image/webp' → false (has html)"
  [{:keys [types has-wildcard?]}]
  (and (not has-wildcard?)
       (not (contains? types "text/html"))
       (not (contains? types "application/json"))
       (every? #(str/starts-with? % "image/") types)
       (seq types)))
