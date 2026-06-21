import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    // 配置路径别名，@ 指向 src 目录
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    // 开发服务器端口
    port: 5173,
    // 允许局域网内其他设备访问
    host: true
  }
})
