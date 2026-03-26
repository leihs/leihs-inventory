(ns leihs.inventory.server.constants)

(def ACTIVATE-CSRF true)
(def APPLY_API_ENDPOINTS_NOT_USED_IN_FE true)
(def APPLY_DEV_ENDPOINTS false)
(def MAX_REQUEST_BODY_SIZE_MB 100)
(def HIDE_BASIC_ENDPOINTS
  "- sign-in / sign-out (html/endpoints)
- csrf-token / test-endpoints
- session-endpoints
- token-endpoints
" true)

(def INVENTORY_VIEW_PATH "/inventory/")

;; nil sets "no cache"-control
(def IMAGE_RESPONSE_CACHE_CONTROL "public, max-age=2592000, immutable") ;30days

(def PROPERTIES_PREFIX "properties_")

(def GENERAL_BUILDING_UUID #uuid "abae04c5-d767-425e-acc2-7ce04df645d1")

(def ACCEPT-CSV "text/csv")
(def ACCEPT-EXCEL "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")

(def ALL-CURRENCIES
  ["AED" "AFN" "ALL" "AMD" "ANG" "AOA" "ARS" "AUD" "AWG" "AZN"
   "BAM" "BBD" "BDT" "BGN" "BHD" "BIF" "BMD" "BND" "BOB" "BRL"
   "BSD" "BTN" "BWP" "BYR" "BZD" "CAD" "CDF" "CHF" "CLP" "CNY"
   "COP" "CRC" "CUC" "CUP" "CVE" "CZK" "DJF" "DKK" "DOP" "DZD"
   "EGP" "ERN" "ETB" "EUR" "FJD" "FKP" "GBP" "GEL" "GHS" "GIP"
   "GMD" "GNF" "GTQ" "GYD" "HKD" "HNL" "HRK" "HTG" "HUF" "IDR"
   "ILS" "INR" "IQD" "IRR" "ISK" "JMD" "JOD" "JPY" "KES" "KGS"
   "KHR" "KMF" "KPW" "KRW" "KWD" "KYD" "KZT" "LAK" "LBP" "LKR"
   "LRD" "LSL" "LYD" "MAD" "MDL" "MGA" "MKD" "MMK" "MNT" "MOP"
   "MRO" "MUR" "MVR" "MWK" "MXN" "MYR" "MZN" "NAD" "NGN" "NIO"
   "NOK" "NPR" "NZD" "OMR" "PAB" "PEN" "PGK" "PHP" "PKR" "PLN"
   "PYG" "QAR" "RON" "RSD" "RUB" "RWF" "SAR" "SBD" "SCR" "SDG"
   "SEK" "SGD" "SHP" "SLL" "SOS" "SRD" "STD" "SVC" "SYP" "SZL"
   "THB" "TJS" "TMT" "TND" "TOP" "TRY" "TTD" "TWD" "TZS" "UAH"
   "UGX" "USD" "UYU" "UZS" "VEF" "VND" "VUV" "WST" "XAF" "XCD"
   "XOF" "XPF" "YER" "ZAR" "ZMW" "ZWR"])
