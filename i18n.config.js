import i18n from "i18next"
import { initReactI18next } from "react-i18next"
import de from "./resources/public/inventory/assets/locales/de/translation.json"
import en from "./resources/public/inventory/assets/locales/en/translation.json"

import * as z from "zod"

console.debug("i18n config loading...")
const resources = {
  de: { translation: de },
  en: { translation: en },
}

// Initialize i18n synchronously with default language
i18n.use(initReactI18next).init({
  resources,
  lng: "de-CH",
  supportedLngs: ["de", "de-CH", "en", "en-GB"],
  load: "languageOnly",
  fallbackLng: ["de"],
  debug: true,
})

// Set up language change handler
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

// Fetch the user profile asynchronously and update the language
fetch("/inventory/profile/", {
  method: "GET",
  headers: {
    Accept: "application/json",
  },
})
  .then((response) => response.json())
  .then((profile) => {
    const locale = profile?.user_details.language_locale || "de-CH"
    if (i18n.language !== locale) {
      return i18n.changeLanguage(locale)
    }
  })
  .catch((error) => {
    console.error("Error fetching profile:", error)
  })

export { i18n }
export default i18n
