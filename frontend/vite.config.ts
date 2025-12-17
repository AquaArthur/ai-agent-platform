import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  // 统一代理目标策略：
  // - 默认：本地 npm run dev 时，代理到宿主机后端 http://localhost:8081
  // - 如存在环境变量 / .env 中的 VITE_PROXY_TARGET，则优先生效；
  //   其中「进程环境变量」优先级高于 .env 文件（便于 Docker 覆盖）
  const proxyTarget =
    process.env.VITE_PROXY_TARGET ||
    env.VITE_PROXY_TARGET ||
    'http://localhost:8081'

  return {
    plugins: [vue()],
    resolve: { alias: { '@': path.resolve(__dirname, 'src') } },
    server: {
      proxy: {
        '/api': {
          target: proxyTarget,
          changeOrigin: true,
        }
      }
    }
  }
})
