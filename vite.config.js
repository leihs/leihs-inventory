import { defineConfig } from "vite"
import react from "@vitejs/plugin-react"
import tsconfigPaths from "vite-tsconfig-paths"
import { createProxyMiddleware } from "http-proxy-middleware"
import { handleI18NextRequest } from "vite-plugin-i18next-save-missing"
import { createFilter } from "vite"

const i18nSaveMissingConfig = {
  locales: ["de", "en", "fr", "es"],
  path: "resources/public/inventory/static/locales",
  namespace: "translation",
  translate: true,
  translateFrom: "de",
}

function consoleWatcher() {
  return {
    name: "console-watcher",
    configureServer(server) {
      // Intercept the console.log method
      const originalLog = console.log
      console.log = function(...args) {
        // Call the original log method
        originalLog.apply(console, args)

        // Parse the messages
        const message = args.join(" ")
        if (message.includes("------ WARNING")) {
          // Handle the warning message (e.g., log to a file, alert, etc.)
          console.warn("Parsed Warning:", message)
        }
      }
    },
  }
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

/** @type {import('vite').UserConfig} */
export default defineConfig({
  plugins: [
    react(),
    tsconfigPaths(),
    proxyAPIRequests(),
    handleI18NextRequest(i18nSaveMissingConfig),
    consoleWatcher(),
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
      // ignored: "**/*.cljs",
    },
  },
})
