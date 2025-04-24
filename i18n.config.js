import i18n from "i18next"
import { initReactI18next } from "react-i18next"
// import Backend from "i18next-http-backend"
// import LanguageDetector from "i18next-browser-languagedetector"
// import { i18nextPlugin } from "translation-check"
import { z } from "zod"
import { makeZodI18nMap } from "zod-i18n-map"

import de from "./resources/public/inventory/assets/locales/de/translation.json"
import en from "./resources/public/inventory/assets/locales/en/translation.json"
import zodDe from "./resources/public/inventory/assets/locales/de/zod.json"
import zodEn from "./resources/public/inventory/assets/locales/en/zod.json"

const resources = {
  de: { translation: de, zod: zodDe },
  en: { translation: en, zod: zodEn },
}

i18n
  // .use(i18nextPlugin)
  // load translation using http -> see /public/locales (i.e. https://github.com/i18next/react-i18next/tree/master/example/react/public/locales)
  // learn more: https://github.com/i18next/i18next-http-backend
  // want your translations to be loaded from a professional CDN? => https://github.com/locize/react-tutorial#step-2---use-the-locize-cdn
  // .use(Backend)
  // detect user language
  // learn more: https://github.com/i18next/i18next-browser-languageDetector
  // .use(LanguageDetector)
  // pass the i18n instance to react-i18next.
  .use(initReactI18next)
  // init i18next
  // for all options read: https://www.i18next.com/overview/configuration-options
  .init({
    resources,
    lng: "de-CH",
    fallbackLng: ["de-CH"],
    debug: true,
  })

z.setErrorMap(makeZodI18nMap({ t: i18n.t.bind(i18n) }))

export { i18n }
export default i18n
