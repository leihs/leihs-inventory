import i18n from "i18next"
import { initReactI18next } from "react-i18next"
import Backend from "i18next-http-backend"
// import LanguageDetector from "i18next-browser-languagedetector"
// import { i18nextPlugin } from "translation-check"

i18n
  // .use(i18nextPlugin)
  // load translation using http -> see /public/locales (i.e. https://github.com/i18next/react-i18next/tree/master/example/react/public/locales)
  // learn more: https://github.com/i18next/i18next-http-backend
  // want your translations to be loaded from a professional CDN? => https://github.com/locize/react-tutorial#step-2---use-the-locize-cdn
  .use(Backend)
  // detect user language
  // learn more: https://github.com/i18next/i18next-browser-languageDetector
  // .use(LanguageDetector)
  // pass the i18n instance to react-i18next.
  .use(initReactI18next)
  // init i18next
  // for all options read: https://www.i18next.com/overview/configuration-options
  .init({
    lng: "de",
    fallbackLng: ["de", "en", "fr", "es"],
    supportedLngs: ["de", "en", "fr", "es"],
    preload: import.meta.env.DEV ? ["en", "de", "fr", "es"] : [],
    load: "languageOnly",
    debug: true,
    saveMissing: true,
    saveMissingPlurals: true,
    backend: {
      loadPath: "/inventory/static/locales/{{lng}}/{{ns}}.json",
    },
  })

export { i18n }
export default i18n
