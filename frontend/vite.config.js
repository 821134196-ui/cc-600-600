import { defineConfig } from 'vite'
import { svelte } from '@sveltejs/vite-plugin-svelte'

export default defineConfig({
  plugins: [svelte()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
      '/mock-api': 'http://localhost:8080'
    }
  },
  build: {
    // 直接构建到 Quarkus 静态资源目录，后端即可托管前端
    outDir: '../backend/src/main/resources/META-INF/resources',
    emptyOutDir: true
  }
})
