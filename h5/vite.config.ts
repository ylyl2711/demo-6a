import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 后端路径本身带 /api 前缀，因此不做 rewrite，仅做转发
const proxy = {
  '^/api': {
    target: 'http://localhost:8080',
    changeOrigin: true
  },
  '^/uploads': {
    target: 'http://localhost:8080',
    changeOrigin: true
  }
} as const

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: '0.0.0.0',
    port: 5174,
    proxy
  },
  preview: {
    host: '0.0.0.0',
    port: 5174,
    proxy
  }
})
