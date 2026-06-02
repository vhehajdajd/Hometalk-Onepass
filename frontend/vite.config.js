import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/hometop/api': {
        target: 'http://localhost:8090',
        changeOrigin: true,
      },
    },
  },
})
