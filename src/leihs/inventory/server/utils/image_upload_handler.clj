(ns leihs.inventory.server.utils.image-upload-handler
  (:require
   [clojure.string :as str]
   [leihs.inventory.server.resources.pool.attachments.constants :refer [config-get]])
  (:import
   [java.io ByteArrayOutputStream File FileInputStream]
   [java.util Base64]
   [org.im4java.core IMOperation ImageCommand]))

(defn resize-image
  "Resize an image using IM4Java and ImageMagick6.9"
  [input-path output-path width height]
  (let [cmd (ImageCommand. (into-array String ["convert"]))
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
  (let [resolved-path (if (map? file-path) (:tempfile file-path) file-path)]
    (when (and resolved-path (.exists (File. resolved-path)))
      (with-open [input-stream (FileInputStream. resolved-path)
                  baos (ByteArrayOutputStream.)]
        (let [buffer (byte-array 1024)]
          (loop []
            (let [bytes-read (.read input-stream buffer)]
              (when (pos? bytes-read)
                (.write baos buffer 0 bytes-read)
                (recur))))
          (.encodeToString (Base64/getEncoder) (.toByteArray baos)))))))

(defn- add-thumb-to-filename [filename]
  (let [[name ext] (str/split filename #"\.(?=[^.]+$)")]
    (str name "_thumb." ext)))

(defn resize-and-convert-to-base64
  "Resize the image, convert it to Base64, and get the file size."
  [input-path]
  (let [width (config-get :api :images :thumbnail :width-px)
        height (config-get :api :images :thumbnail :height-px)
        output-path (add-thumb-to-filename input-path)]
    (resize-image input-path output-path width height)
    {:base64 (file-to-base64 output-path)
     :file-size (get-file-size output-path)}))
