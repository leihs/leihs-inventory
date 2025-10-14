import i18n from "i18next"
import { initReactI18next } from "react-i18next"
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

  i18n.on("languageChanged", async (lng) => {
    if (lng.startsWith("de")) {
      const { default: de } = await import(
        "./resources/public/inventory/assets/locales/de/zod-localization.js"
      )
      z.config(de())
    } else {
      const { default: en } = await import(
        "./resources/public/inventory/assets/locales/en/zod-localization.js"
      )
      z.config(en())
    }
  })
})()

export { i18n }
export default i18n
