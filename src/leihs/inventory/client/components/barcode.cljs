(ns leihs.inventory.client.components.barcode
  (:require
   ["jsbarcode" :default JsBarcode]
   [uix.core :as uix :refer [$ defui]]))

(defui main
  "Barcode component that renders barcodes using jsbarcode library.
   
   Props:
   - value: The barcode value to encode (required)
   - elementType: 'svg', 'canvas', or 'img' (default: 'svg')
   - format: Barcode format like CODE128, EAN13, CODE39, etc.
   - width: Width of individual bars (default: 2)
   - height: Height of barcode (default: 100)
   - displayValue: Show text below barcode (default: true)
   - fontSize: Font size for text (default: 20)
   - margin: Margin around barcode (default: 10)
   - className: Additional CSS classes
   - background: Background color (default: '#ffffff')
   - lineColor: Bar color (default: '#000000')
   
   Additional options: text, textMargin, font, fontOptions, 
   textAlign, textPosition, valid (callback)"

  [{:keys [value elementType format width height displayValue
           fontSize margin className background lineColor
           text textMargin font fontOptions textAlign
           textPosition valid]

    :or {elementType "svg"
         format "CODE128"
         width 2
         height 100
         displayValue true
         fontSize 20
         margin 10
         background "#ffffff"
         lineColor "#000000"}}]

  (let [element-ref (uix/use-ref nil)
        element-props {:ref element-ref
                       :data-test-id (str "barcode-" value)
                       :class-name className}]

    ;; Render barcode when component mounts or props change
    (uix/use-effect
     (fn []
       (when-let [element (.-current element-ref)]
         (try
           ;; Build options object, only including defined values
           (let [options (clj->js
                          (merge
                           {:format format
                            :width width
                            :height height
                            :displayValue displayValue
                            :fontSize fontSize
                            :margin margin
                            :background background
                            :lineColor lineColor}
                           (when text {:text text})
                           (when textMargin {:textMargin textMargin})
                           (when font {:font font})
                           (when fontOptions {:fontOptions fontOptions})
                           (when textAlign {:textAlign textAlign})
                           (when textPosition {:textPosition textPosition})
                           (when valid {:valid valid})))]

             ;; Call JsBarcode with element and options
             (JsBarcode element value options))

           (catch js/Error e
             (js/console.error "Barcode generation error:" e)))))

     ;; Re-run effect when any of these values change
     [value format width height displayValue fontSize margin
      background lineColor text textMargin font fontOptions
      textAlign textPosition valid])

    ;; Render appropriate element type
    (case elementType
      "canvas" ($ :canvas element-props)
      "img" ($ :img element-props)
      "svg" ($ :svg element-props))))

;; Export React-compatible version
(def Barcode
  (uix/as-react
   (fn [props]
     (main props))))
