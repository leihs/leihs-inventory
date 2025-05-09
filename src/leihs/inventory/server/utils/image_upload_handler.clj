(ns leihs.inventory.server.utils.image-upload-handler
  (:require
   [byte-streams :as bs]
   [clojure.string :as str]
   [clojure.walk :refer [keywordize-keys]]
   [leihs.core.anti-csrf.back :as anti-csrf]
   [leihs.core.constants :as constants]
   [leihs.core.core :refer [presence]]
   [leihs.core.db :as db]
   [leihs.core.json :refer [to-json]]
   [leihs.core.ring-audits :as ring-audits]
   [leihs.core.routing.back :as core-routing]
   [leihs.core.routing.dispatch-content-type :as dispatch-content-type]
   [leihs.core.sign-in.back :as be]
   [leihs.inventory.server.constants :as consts]
   [leihs.inventory.server.utils.config :refer [initialize get-config]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [ring.util.codec :as codec]
   [ring.util.response :as response]
   [taoensso.timbre :refer [error]])
  (:import [java.io File]
           [java.io File FileInputStream ByteArrayOutputStream]
           [java.net URL JarURLConnection]
           (java.time LocalDateTime)
           [java.util Base64]
           [java.util UUID]
           [java.util.jar JarFile]
           [org.im4java.core ConvertCmd IMOperation]
           [org.im4java.core IMOperation ImageCommand]
           [org.im4java.process ProcessStarter]))

;(defn resize-image
;  "Resize an image using IM4Java."
;  [input-path output-path width height]
;
;  (println ">o> abc.resize-image.in" input-path)
;  (println ">o> abc.resize-image.out"  output-path )
;  (println ">o> abc.resize-image.with"  width )
;  (println ">o> abc.resize-image.height" height)
;
;  (let [cmd (ConvertCmd.)
;        op (IMOperation.)]
;    (.addImage op (into-array String [input-path]))
;    (.resize op (int width) (int height))
;    (.addImage op (into-array String [output-path]))
;    (.run cmd op (into-array String []))))  ;; Corrected invocation

(defn resize-image
  "Resize an image using IM4Java and ImageMagick v7."
  [input-path output-path width height]
  (println ">o> abc.resize-image" input-path output-path width height)

  (println ">o> abc.resize-image.in" input-path (type input-path))
  (println ">o> abc.resize-image.out" output-path (type output-path))
  (println ">o> abc.resize-image.with" width (type width))
  (println ">o> abc.resize-image.height" height (type height))

  ;(let [cmd (ImageCommand. "magick")
  (let [cmd (ImageCommand. (into-array String ["magick"]))
        op (IMOperation.)]
    (.addImage op (into-array String [input-path]))
    (.resize op (int width) (int height))
    (.addImage op (into-array String [output-path]))
    (.run cmd op (into-array String []))))

(defn get-file-size
  "Get the file size in bytes."
  [file-path]
  (.length (File. file-path)))

(defn file-to-base64
  "Convert a file to a Base64 string."
  [file-path]

  (println ">o> abc.to-base" file-path)
  (println ">o> abc.to-base >>>> " (:tempfile file-path))

  (with-open [input-stream (FileInputStream. (:tempfile file-path)) ;; Convert to string path
              baos (ByteArrayOutputStream.)]
    (let [buffer (byte-array 1024)]
      (loop []
        (let [bytes-read (.read input-stream buffer)]
          (when (pos? bytes-read)
            (.write baos buffer 0 bytes-read)
            (recur))))
      (let [encoder (Base64/getEncoder)]
        (.encodeToString encoder (.toByteArray baos))))))

(defn extract-file-path
  "Extracts the file path from a string or map with :tempfile key."
  [file-path]
  (cond
    (string? file-path) file-path
    (map? file-path) (get file-path :tempfile)
    :else nil))

(defn file-to-base64
  "Convert a file to a Base64 string."
  [file-path]
  (let [resolved-path (extract-file-path file-path)]

    (println ">o> abc.to-base: " resolved-path)
    (println ">o> abc.to-base >>>> " (if (map? file-path) (:tempfile file-path) "N/A"))

    (if (and resolved-path (.exists (File. resolved-path)))
      (with-open [input-stream (FileInputStream. resolved-path)
                  baos (ByteArrayOutputStream.)]
        (let [buffer (byte-array 1024)]
          (loop []
            (let [bytes-read (.read input-stream buffer)]
              (when (pos? bytes-read)
                (.write baos buffer 0 bytes-read)
                (recur))))
          (let [encoder (Base64/getEncoder)]
            (.encodeToString encoder (.toByteArray baos)))))
      (do
        (println "Error: Invalid file path or file does not exist:" resolved-path)
        nil))))

(defn- add-thumb-to-filename [filename]
  (let [[name ext] (str/split filename #"\.(?=[^.]+$)")]
    (str name "_thumb." ext)))

(defn resize-and-convert-to-base64
  "Resize the image, convert it to Base64, and get the file size."
  ;[input-path width height]
  [input-path]
  (let [upload-dir (get-in (get-config) [:api :upload-dir])
        width (get-in (get-config) [:api :images :thumbnail :width-px])
        height (get-in (get-config) [:api :images :thumbnail :height-px])

;output-path (str CONST_FILE_PATH "resized_output.png")]

        output-path (add-thumb-to-filename input-path)
        p (println ">o> abc.output-path1" output-path)

        ;output-path (str upload-dir (add-thumb-to-filename input-path))
        ;
        ;p (println ">o> abc.input-path2" input-path)
        ;p (println ">o> abc.output-path2" output-path)
        ]
    (resize-image input-path output-path width height)
    (let [p (println ">o> abc.output-path" output-path)

          file-size (get-file-size output-path)

          p (println ">o> abc.file-size" file-size)

          base64-str (file-to-base64 output-path)

          p (println ">o> abc.base64-str" base64-str)]

      {:base64 base64-str
       :file-size file-size})))