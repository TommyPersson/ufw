import { defineConfig } from "vite"
import react from "@vitejs/plugin-react"

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  build: {
    assetsDir: "admin/ui/assets",
  },
  resolve: {
  },
  server: {
    proxy: {
      "/admin/api": {
        target: "http://localhost:8081",
        changeOrigin: true,
        secure: false,
      }
    }
  }
})
