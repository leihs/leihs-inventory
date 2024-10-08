
(ns leihs.inventory.server.resources.images.main
  (:require

   [clojure.java.io :as io]

   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [path-params]]
   [clojure.string :as str]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]]

   )

  (:import [java.util Base64]
   [java.io ByteArrayInputStream]))



(defn pr [str fnc]
  ;(println ">oo> HELPER / " str fnc)(println ">oo> HELPER / " str fnc)
  (println ">oo> " str fnc)
  fnc
  )



(defn clean-base64-string [base64-str]
  (clojure.string/replace base64-str #"\s+" ""))



;(defn url-safe-to-standard-base64 [base64-str]
;  (-> base64-str
;    (clojure.string/replace "-" "+")
;    (clojure.string/replace "_" "/")))
;
;
;
;(defn add-padding [base64-str]
;  (let [mod (mod (count base64-str) 4)]
;    (cond
;      (= mod 2) (str base64-str "==")
;      (= mod 3) (str base64-str "=")
;      :else base64-str)))
;
;
;(defn decode-base64-str [base64-str]
;  (let [cleaned-str (-> base64-str
;                      clean-base64-string
;                      url-safe-to-standard-base64
;                      add-padding)
;        decoder (Base64/getDecoder)]
;    (.decode decoder cleaned-str)))
;
;(defn save-base64-image-to-file [base64-str output-filepath]
;  (try
;    (let [decoded-bytes (decode-base64-str base64-str)]
;      (with-open [output-stream (io/output-stream output-filepath)]
;        (.write output-stream decoded-bytes)))
;    (catch IllegalArgumentException e
;      (println "Failed to decode Base64 string:" (.getMessage e)))))
;
;
;
;(defn serve-image-file [filepath content-type]
;  {:status 200
;   :headers {"Content-Type" content-type
;             "Content-Disposition" (str "inline; filename=\"" (.getName (io/file filepath)) "\"")}
;   :body (io/input-stream filepath)})
;
;(defn handle-base64-image-request [result]
;  (let [output-filepath (:filename result)
;        content-type (:content_type result)
;        base64-str (:content result)]
;    (save-base64-image-to-file base64-str output-filepath)
;    (serve-image-file output-filepath content-type)))











;(ns your-namespace
;  (:require [clojure.java.io :as io])
;  (:import (java.util Base64)))

(defn clean-base64-string [base64-str]
  (clojure.string/replace base64-str #"\s+" ""))

(defn url-safe-to-standard-base64 [base64-str]
  (-> base64-str
    (clojure.string/replace "-" "+")
    (clojure.string/replace "_" "/")))

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

(defn handle-base64-image-request [result]
  (try
    (let [content-type (:content_type result)
          base64-str (:content result)

          p (println ">o> base64-str" base64-str)

          decoded-bytes (decode-base64-str base64-str)]
      {:status 200
       :headers {"Content-Type" content-type
                 "Content-Disposition" "inline"}
       :body (io/input-stream (java.io.ByteArrayInputStream. decoded-bytes))})
    (catch IllegalArgumentException e
      {:status 400
       :body (str "Failed to decode Base64 string: " (.getMessage e))})))












(defn create-image-response [result]
  {:status 200
   :headers {"Content-Type" (:content_type result)
             "Content-Length" (str (:size result))          ; Ensure size is converted to string
             "Content-Disposition" (str "inline; filename=\"" (:filename result) "\";")}
   :body (:data result)})                                   ; Directly assigns the data without decoding


(defn create-image-response [result]
  {:status 200
   ;:headers {"Content-Type" (:content_type result)
   ;          "Content-Length" (str (:size result)) ; Ensure size is converted to string
   ;          "Content-Disposition" (str "inline; filename=\"" (:filename result) "\";")}
   ;:body (:data result)}) ; Directly assigns the data without decoding
   :body result})                                           ; Directly assigns the data without decoding



;(defn get-images-handler [request]
;  (try
;    (let [tx (:tx request)
;
;
;          accept-header (get-in request [:headers "accept"])
;          is-jpeg? (= accept-header "image/jpeg")
;          json-request? (= accept-header "application/json")
;
;
;          p (println ">o> ????")
;          ;pool_id (-> request path-params :pool_id)
;          image_id (-> request path-params :id)
;
;
;          p (println ">o> request.keys1 =>>" (keys request))
;          p (println ">o> request.keys2 =>>" (:headers request))
;          p (println ">o> request.keys3 =>>" (get request [:headers "accept"]))
;
;          is-thumbnail? false
;
;          p (println ">o> >>>" image_id is-thumbnail?)
;
;          query (-> (sql/select :i.*)
;                  ;query (-> (sql/select :i.target_id)
;                  (sql/from [:images :i])
;                  ;(sql/where [:= :i.inventory_pool_id pool_id])
;                  ;(cond-> image_id (sql/where [:= :i.id item_id]))
;                  (sql/where [:= :i.thumbnail is-thumbnail?])
;
;
;                  (cond-> image_id
;
;                    (sql/where [:= :i.target_id
;
;                                (-> (sql/select :i.target_id)
;                                  (sql/from [:images :i])
;                                  (sql/where [:= :i.id image_id])
;
;                                  )
;
;                                ])
;                    )
;
;
;                  (sql/limit 2)
;                  sql-format)
;
;
;          ;query (-> (sql/select :i.target_id)
;          ;        (sql/from [:images :i])
;          ;        (sql/where [:= :i.id item_id])
;          ;        sql-format)
;          ;
;          ;query (-> (sql/select :i.target_id)
;          ;        (sql/from [:images :i])
;          ;        ;(sql/where [:= :i.inventory_pool_id pool_id])
;          ;        ;(cond-> image_id (sql/where [:= :i.id item_id]))
;          ;        (sql/where [:= :i.id item_id])
;          ;        ;(sql/limit 10)
;          ;        sql-format)
;
;          p (println ">o> query" query)
;
;
;
;
;          result (jdbc/query tx query)
;
;          ;result (first result)
;
;          ;p (println ">o> result" result)
;          ]
;      ;(response result)
;
;      ;{:status 200, :headers {
;      ;                        "Content-Type" (:content_type result)
;      ;                        "Content-Length" (:size result)
;      ;                        "Content-Disposition" (str "inline; filename=\"" (:filename result) "\";")
;      ;                        ;"Content-Transfer-Encoding" "binary"
;      ;                        ;}, :body (decode-base64 (:data result))}
;      ;                        }, :body  (:data result)}
;
;
;      ;(serve-image-base64-url result)
;      ;(serve-base64-image result)
;
;
;      (cond
;        ;(and json-request?  image_id) (pr ">o>2"(response  {:data result})) ;one
;        (and json-request? image_id) (pr ">o>2" (response result)) ;one
;        ;(and json-request?  image_id) (pr ">o>2"{        :headers {"Content-Type" "application/json"}
;        ;                                         :data result
;        ;                                         :status 200
;        ;                                         }) ;one
;
;        (and json-request? (nil? image_id)) (pr ">o>1" (response {:data result})) ;;all
;        (and (not json-request?) image_id)
;
;        ;(create-image-response (first result)
;        (pr ">o>3" (handle-base64-image-request (first result)))
;        ;(pr ">o>3" (handle-base64-image-request (:content (first result))))
;        )
;
;
;      ;)
;
;
;      ;(if      (= accept-header "image/jpeg")
;      ;(handle-base64-image-request (:content result))
;      ; )
;
;      )
;    (catch Exception e
;      (error "Failed to get pools of user" e)
;      (bad-request {:error "Failed to get pools of user" :details (.getMessage e)}))))






(defn get-image-thumbnail-handler [request]
  (try
    (let [tx (:tx request)


          accept-header (get-in request [:headers "accept"])
          is-jpeg? (= accept-header "image/jpeg")
          json-request? (= accept-header "application/json")


          p (println ">o> ????")
          ;pool_id (-> request path-params :pool_id)
          image_id (-> request path-params :id)


          p (println ">o> request.keys1 =>>" (keys request))
          p (println ">o> request.keys2 =>>" (:headers request))
          p (println ">o> request.keys3 =>>" (get request [:headers "accept"]))

          ;is-thumbnail? true
          is-thumbnail? (str/ends-with? (:uri request) "/thumbnail")

          p (println ">o> >>>" image_id is-thumbnail?)

          query (-> (sql/select :i.*)
                  ;query (-> (sql/select :i.target_id)
                  (sql/from [:images :i])
                  ;(sql/where [:= :i.inventory_pool_id pool_id])
                  ;(cond-> image_id (sql/where [:= :i.id item_id]))
                  (sql/where [:= :i.thumbnail is-thumbnail?])

                  (cond-> is-thumbnail? (sql/where [:= :i.thumbnail is-thumbnail?] ))

                  (cond-> image_id
                    (sql/where [:= :i.target_id

                                (-> (sql/select :i.target_id)
                                  (sql/from [:images :i])
                                  (sql/where [:= :i.id image_id])

                                  )

                                ])
                    )


                  (sql/limit 2)
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

          ;result (first result)

          ;p (println ">o> result" result)
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


      (cond
        ;(and json-request?  image_id) (pr ">o>2"(response  {:data result})) ;one
        (and json-request? image_id) (pr ">o>2" (response result)) ;one
        ;(and json-request?  image_id) (pr ">o>2"{        :headers {"Content-Type" "application/json"}
        ;                                         :data result
        ;                                         :status 200
        ;                                         }) ;one

        (and json-request? (nil? image_id)) (pr ">o>1" (response {:data result})) ;;all
        (and (not json-request?) image_id)

        ;(create-image-response (first result)
        ;(pr ">o>3" (handle-base64-image-request (:content (first result))))

        (pr ">o>3" (handle-base64-image-request (first result)))


        )


      ;)


      ;(if      (= accept-header "image/jpeg")
      ;(handle-base64-image-request (:content result))
      ; )

      )
    (catch Exception e
      (error "Failed to get pools of user" e)
      (bad-request {:error "Failed to get pools of user" :details (.getMessage e)}))))
