;(ns leihs.inventory.server.resources.images.main
;  (:require
;   [clojure.data.codec.base64 :as b64]
;   ;[clojure.java.io :as io]
;
;   [clojure.set]
;
;   [honey.sql :refer [format] :rename {format sql-format}]
;   [honey.sql.helpers :as sql]
;   [leihs.inventory.server.resources.utils.request :refer [path-params]]
;   [next.jdbc.sql :as jdbc]
;
;   [ring.middleware.accept]
;   [ring.util.response :refer [bad-request response status]]
;   [taoensso.timbre :refer [error]])
;
;  (:import [java.util Base64]
;    [java.io ByteArrayInputStream])
;  )


(ns leihs.inventory.server.resources.images.main
  (:require
   ;[clojure.data.codec.base64 :as b64]  ; Remove this line

   [clojure.set]

   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [path-params]]
   [next.jdbc.sql :as jdbc]

   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]]
  [clojure.java.io :as io]

  ;(require '[clojure.java.io :as io])
  ;(import '[java.util Base64]
   )

  (:import [java.util Base64]
   [java.io ByteArrayInputStream]))


(defn ->when [x pred y]
  (if pred
    (-> x y)
    x))

;(defn decode-base64 [base64-str]
;  (let [decoder (b64/decode (.getBytes base64-str "UTF-8"))]
;    (io/input-stream decoder)))


;(defn serve-image-base64-url [result]
;  (let [base64-content (:content result)
;        content-type (:content_type result)
;        data-url (str "data:" content-type ";base64," base64-content)]
;    {:status 200
;     ;:headers {"Content-Type" "text/plain"} ; Optional: specify a content type that displays raw text or data
;     :headers {"Content-Type" "image/jpeg"} ; Optional: specify a content type that displays raw text or data
;     :body data-url}))
;
;(defn decode-base64 [base64-str]
;  (let [decoded-bytes (.decode (Base64/getDecoder) base64-str)]
;    (ByteArrayInputStream. decoded-bytes)))
;
;
;(defn serve-base64-image [result]
;  {:status 200
;   :headers {"Content-Type" (:content_type result)
;             "Content-Disposition" (str "inline; filename=\"" (:filename result) "\";")
;             "Content-Transfer-Encoding" "base64"}
;   ;:body (:content result)}) ; directly sending the Base64 content as body
;   :body (encode (.getBytes (:content result)))
;   }) ; directly sending the Base64 content as body

(defn ->when [x pred y]
  (if pred
    (-> x y)
    x))





(defn clean-base64-string [base64-str]
  (clojure.string/replace base64-str #"\s+" ""))



(defn url-safe-to-standard-base64 [base64-str]
  (-> base64-str
    (clojure.string/replace "-" "+")
    (clojure.string/replace "_" "/")))



(defn save-base64-image-to-file [base64-str output-filepath]
  (let [decoder (Base64/getDecoder)
        decoded-bytes (.decode decoder base64-str)]
    (with-open [output-stream (io/output-stream output-filepath)]
      (.write output-stream decoded-bytes))))

(defn add-padding [base64-str]
  (let [mod (mod (count base64-str) 4)]
    (cond
      (= mod 2) (str base64-str "==")
      (= mod 3) (str base64-str "=")
      :else base64-str)))


(defn decode-base64-str [base64-str]
  (let [cleaned-str (-> base64-str
                      clean-base64-string
                      url-safe-to-standard-base64
                      add-padding)
        decoder (Base64/getDecoder)]
    (.decode decoder cleaned-str)))

(defn save-base64-image-to-file [base64-str output-filepath]
  (try
    (let [decoded-bytes (decode-base64-str base64-str)]
      (with-open [output-stream (io/output-stream output-filepath)]
        (.write output-stream decoded-bytes)))
    (catch IllegalArgumentException e
      (println "Failed to decode Base64 string:" (.getMessage e)))))



(defn serve-image-file [filepath content-type]
  {:status 200
   :headers {"Content-Type" content-type
             "Content-Disposition" (str "inline; filename=\"" (.getName (io/file filepath)) "\"")}
   :body (io/input-stream filepath)})

(defn handle-base64-image-request [base64-str]
  (let [output-filepath "output-image.jpg"
        content-type "image/jpeg"]
    (save-base64-image-to-file base64-str output-filepath)
    (serve-image-file output-filepath content-type)))



(defn get-images-handler [request]
  (try
    (let [tx (:tx request)


          accept-header (get-in request [:headers "accept"])


          p (println ">o> ????")
          ;pool_id (-> request path-params :pool_id)
          image_id (-> request path-params :id)


          p (println ">o> request.keys1 =>>" (keys request))
          p (println ">o> request.keys2 =>>" (:headers request))
          p (println ">o> request.keys3 =>>" (get request [:headers "accept"]))

          is-thumbnail? false

          p (println ">o> >>>" image_id is-thumbnail?)

          query (-> (sql/select :i.*)
          ;query (-> (sql/select :i.target_id)
                  (sql/from [:images :i])
                  ;(sql/where [:= :i.inventory_pool_id pool_id])
                  ;(cond-> image_id (sql/where [:= :i.id item_id]))
                  (sql/where [:= :i.thumbnail is-thumbnail?])


                  (cond-> image_id

                  (sql/where [:= :i.target_id

                              (-> (sql/select :i.target_id)
                                (sql/from [:images :i])
                                (sql/where [:= :i.id image_id])

                                )

                              ])
                    )


                  ;(sql/limit 10)
                  sql-format)


          ;query (-> (sql/select :i.target_id)
          ;        (sql/from [:images :i])
          ;        (sql/where [:= :i.id item_id])
          ;        sql-format)
          ;
          ;query (-> (sql/select :i.target_id)
          ;        (sql/from [:images :i])
          ;        ;(sql/where [:= :i.inventory_pool_id pool_id])
          ;        ;(cond-> image_id (sql/where [:= :i.id item_id]))
          ;        (sql/where [:= :i.id item_id])
          ;        ;(sql/limit 10)
          ;        sql-format)

          p (println ">o> query" query)

          result (jdbc/query tx query)

          result (first result)

          p (println ">o> result" result)
          ]
      ;(response result)

      ;{:status 200, :headers {
      ;                        "Content-Type" (:content_type result)
      ;                        "Content-Length" (:size result)
      ;                        "Content-Disposition" (str "inline; filename=\"" (:filename result) "\";")
      ;                        ;"Content-Transfer-Encoding" "binary"
      ;                        ;}, :body (decode-base64 (:data result))}
      ;                        }, :body  (:data result)}


      ;(serve-image-base64-url result)
      ;(serve-base64-image result)

      (handle-base64-image-request (:content result))

      )
    (catch Exception e
      (error "Failed to get pools of user" e)
      (bad-request {:error "Failed to get pools of user" :details (.getMessage e)}))))
