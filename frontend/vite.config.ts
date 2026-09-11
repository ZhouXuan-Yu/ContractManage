import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@hero-local': fileURLToPath(new URL('../../../HeroUIPro/herouipro-v3/src/components', import.meta.url)),
      '@hero-pro': fileURLToPath(new URL('../../../HeroUIPro/herouipro-v3/src', import.meta.url)),
    },
    dedupe: ['react', 'react-dom'],
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:19090',
        changeOrigin: true,
      },
    },
  },
})
