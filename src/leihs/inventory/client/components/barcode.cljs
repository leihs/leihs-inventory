(ns leihs.inventory.client.components.barcode
  (:require
   ["jsbarcode" :default JsBarcode]
   [uix.core :as uix :refer [$ defui]]))

(defui Barcode
  "Barcode component that renders barcodes using jsbarcode library.
   
   Props:
   - value: The barcode value to encode (required)
   - element-type: 'svg', 'canvas', or 'img' (default: 'svg')
   - format: Barcode format like CODE128, EAN13, CODE39, etc.
   - width: Width of individual bars (default: 2)
   - height: Height of barcode (default: 100)
   - display-value: Show text below barcode (default: true)
   - font-size: Font size for text (default: 20)
   - margin: Margin around barcode (default: 10)
   - class-name: Additional CSS classes
   - background: Background color (default: '#ffffff')
   - line-color: Bar color (default: '#000000')

   Additional options: text, text-margin, font, font-options,
   text-align, text-position, valid (callback)"

  [{:keys [value element-type format width height display-value
           font-size margin class-name background line-color
           text text-margin font font-options text-align
           text-position valid]

    :or {element-type "svg"
         format "CODE128"
         width 2
         height 100
         display-value true
         font-size 20
         margin 10
         background "#ffffff"
         line-color "#000000"}}]

  (let [element-ref (uix/use-ref nil)
        element-props {:ref element-ref
                       :data-test-id (str "barcode-" value)
                       :class-name class-name}]

    ;; Render barcode when component mounts or props change
    (uix/use-effect
     (fn []
       (when-let [element @element-ref]
         (try
           ;; Build options object, only including defined values
           (let [options (clj->js
                          (merge
                           {:format format
                            :width width
                            :height height
                            :displayValue display-value
                            :fontSize font-size
                            :margin margin
                            :background background
                            :lineColor line-color}
                           (when text {:text text})
                           (when text-margin {:textMargin text-margin})
                           (when font {:font font})
                           (when font-options {:fontOptions font-options})
                           (when text-align {:textAlign text-align})
                           (when text-position {:textPosition text-position})
                           (when valid {:valid valid})))]

             ;; Call JsBarcode with element and options
             (JsBarcode element value options))

           (catch js/Error e
             (js/console.error "Barcode generation error:" e)))))

     ;; Re-run effect when any of these values change
     [value format width height display-value font-size margin
      background line-color text text-margin font font-options
      text-align text-position valid])

    ;; Render appropriate element type
    (case element-type
      "canvas" ($ :canvas element-props)
      "img" ($ :img element-props)
      "svg" ($ :svg element-props))))
