import i18n from "i18next"
import { initReactI18next } from "react-i18next"
// import Backend from "i18next-http-backend"
// import LanguageDetector from "i18next-browser-languagedetector"
// import { i18nextPlugin } from "translation-check"

import de from "./resources/public/inventory/assets/locales/de/translation.json"
import en from "./resources/public/inventory/assets/locales/en/translation.json"

import * as z from "zod"
;(async () => {
  const resources = {
    de: { translation: de },
    en: { translation: en },
  }

  let locale = "de-CH"

  try {
    const response = await fetch("/inventory/profile/", {
      method: "GET",
      headers: {
        Accept: "application/json",
      },
    })

    const profile = await response.json()
    locale = profile?.user_details.language_locale || "de-CH"
  } catch (error) {
    console.error("Error fetching profile:", error)
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
      lng: locale,
      supportedLngs: ["de", "de-CH", "en", "en-GB"],
      load: "languageOnly",
      fallbackLng: ["de"],
      debug: true,
    })

  // async function loadLocale(locale) {
  //   const { default: loc } = await import(`zod/v4/locales/${locale}.js`)
  //   z.config(loc())
  // }

  i18n.on("languageChanged", async (lng) => {
    if (lng.startsWith("de")) {
      const { default: de } = await import(
        "./resources/public/inventory/assets/locales/de/de.js"
      )
      z.config(de())
    } else {
      const { default: en } = await import(
        "./resources/public/inventory/assets/locales/en/en.js"
      )
      z.config(en())
    }
  })
})()

export { i18n }
export default i18n
