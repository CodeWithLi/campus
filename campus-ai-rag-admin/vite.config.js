import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    // vueDevTools(),
    vue(),
  ],
  server: {
    proxy: {
      // 代理后端（后端 context-path=/campus）
      // 命中 /campus 下的任意接口都转发，避免路径不一致导致 404(5173)。
      '/campus': {
        // target: 'https://admin.baomi-ai.com',
        target: 'http://localhost:19832',
        changeOrigin: true,
        secure: true,
        ws: true,
      },
    },
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
})
