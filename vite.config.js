import { defineConfig } from "vite"
import react from "@vitejs/plugin-react"
import tsconfigPaths from "vite-tsconfig-paths"
import { createProxyMiddleware } from "http-proxy-middleware"
import { handleI18NextRequest } from "vite-plugin-i18next-save-missing"

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

/** @type {import('vite').UserConfig} */
export default defineConfig({
  plugins: [
    react(),
    tsconfigPaths(),
    proxyAPIRequests(),
    handleI18NextRequest(i18nSaveMissingConfig),
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
