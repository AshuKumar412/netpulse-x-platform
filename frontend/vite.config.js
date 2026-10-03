import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],

  // sockjs-client (v1.6.1) is a legacy CommonJS library that references the Node.js
  // `global` identifier directly. Older bundlers (Webpack 4/5) auto-shimmed global→window.
  // Vite 8 (Rolldown) does not. We define global = globalThis here, which is the correct
  // browser-standard equivalent — globalThis is defined by the ECMAScript spec and equals
  // `window` in browsers. This is a build-time text substitution, not a runtime polyfill injection.
  define: {
    global: 'globalThis',
  },

  server: {
    port: 5173,
    host: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/ws': {
        target: 'http://localhost:8080',
        ws: true
      }
    }
  }
})
