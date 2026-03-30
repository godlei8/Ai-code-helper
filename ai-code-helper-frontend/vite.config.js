import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [vue()],
    server: {
      host: '0.0.0.0',
      port: 5110,
      proxy: {
        '/api': {
          target: env.VITE_PROXY_TARGET || 'http://localhost:8081',
          changeOrigin: true
        },
        '/hot-api/juejin': {
          target: 'https://api.juejin.cn',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/hot-api\/juejin/, '')
        },
        '/hot-api/v2ex': {
          target: 'https://www.v2ex.com',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/hot-api\/v2ex/, '')
        }
      }
    }
  }
})
