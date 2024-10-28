import { defineConfig } from "vite"
import react from "@vitejs/plugin-react"
import tsconfigPaths from "vite-tsconfig-paths"
import { createProxyMiddleware } from "http-proxy-middleware"
import { handleI18NextRequest } from "vite-plugin-i18next-save-missing"
import { createFilter } from "vite"
import puppeteer from "puppeteer"

const i18nSaveMissingConfig = {
  locales: ["de", "en", "fr", "es"],
  path: "resources/public/inventory/static/locales",
  namespace: "translation",
  translate: true,
  translateFrom: "de",
}

const proxyAPIRequests = () => ({
  name: "proxy-api-requests",
  configureServer: (server) => {
    // Add middleware for handling JSON requests
    server.middlewares.use((req, res, next) => {
      const acceptHeader = req.headers.accept || ""

      if (acceptHeader.includes("application/json")) {
        // Create proxy middleware for JSON requests
        const proxy = createProxyMiddleware({
          target: "http://localhost:3260",
          changeOrigin: true,
        })
        return proxy(req, res, next)
      }

      // Proceed to the next middleware if not a JSON request or specific route
      next()
    })
  },
})

// function myPlugin() {
//   return {
//     name: "my-plugin", // required, will show up in warnings and errors
//     configureServer(server) {
//       server.httpServer?.once("listening", () => {
//         console.log("Vite development server is ready!")
//         // Add your custom logic here
//       })
//     },
//   }
// }

async function myPlugin() {
  return {
    name: "my-plugin", // required, will show up in warnings and errors
    handleHotUpdate({ file, server }) {
      // const browser = await puppeteer.launch({ headless: false })
      // const page = await browser.newPage()
      //
      // // Navigate to the desired URL
      // await page.goto("http://localhost:9630/build/frontend/")
      //
      // // Evaluate JavaScript within the page context
      // const result = await page.evaluate(() => {
      //   // Example: Get the title of the page
      //   // Find the element containing the text "Warnings"
      //   const el = document.querySelector(
      //     ".shadow_cljs_ui_components_build_status__L89_C28",
      //   )
      //   // const element = Array.from(document.querySelectorAll("div")).find(
      //   //   (div) => div.textContent.includes("Warnings"),
      //   // )
      //   //
      //   // console.debug("ELEMENT #####", element)
      //   //
      //   // // Return the outer HTML of the parent div
      //   // return element ? element.outerHTML : null
      //   return el
      // })
      //
      // console.log('Parent div containing "Warnings":', result)
      //
      // // Close the browser
      // await browser.close()
      // The browser will remain open
      // console.log(`File updated: ${file}`)
      // fetch("http://localhost:9630/build/frontend/").then((data) =>
      //   console.debug(data),
      // )
    },
  }
}

/** @type {import('vite').UserConfig} */
export default defineConfig({
  plugins: [
    react(),
    tsconfigPaths(),
    proxyAPIRequests(),
    handleI18NextRequest(i18nSaveMissingConfig),
    myPlugin(),
  ],
  publicDir: "../../../../resources/public/inventory/static",
  root: "src/leihs/inventory/client",

  build: {
    outDir: "../../../../resources/public",
    assetsDir: "inventory/assets",
    copyPublicDir: false,
  },

  server: {
    watch: {
      // Exclude .cljs files
      // so changes dont trigger multiple reloads
      ignored: [],
    },
  },
})
