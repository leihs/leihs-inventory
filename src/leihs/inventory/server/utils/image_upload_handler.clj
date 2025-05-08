(ns leihs.inventory.server.utils.image-upload-handler
  (:require
   [byte-streams :as bs]
   [clojure.string :as str]
   [leihs.core.db :as db]
   [leihs.core.json :refer [to-json]]
   [leihs.inventory.server.utils.config :refer [get-config]]
   [ring.util.response :as response])
  (:import [java.io File FileInputStream ByteArrayOutputStream]
           [java.util Base64]
           [org.im4java.core IMOperation ImageCommand]))

(defn resize-image
  "Resize an image using IM4Java and ImageMagick"
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
  (let [upload-dir (get-in (get-config) [:api :upload-dir])
        width (get-in (get-config) [:api :images :thumbnail :width-px])
        height (get-in (get-config) [:api :images :thumbnail :height-px])
        output-path (add-thumb-to-filename input-path)]
    (resize-image input-path output-path width height)
    {:base64 (file-to-base64 output-path)
     :file-size (get-file-size output-path)}))
