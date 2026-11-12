import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    // en développement, /api est envoyé au serveur Spring Boot
    proxy: { '/api': 'http://localhost:8080' },
  },
  build: {
    // le build est servi directement par Spring Boot
    outDir: '../backend/src/main/resources/static',
    emptyOutDir: true,
  },
})
