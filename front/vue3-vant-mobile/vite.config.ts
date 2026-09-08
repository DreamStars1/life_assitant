import path from 'node:path'
import process from 'node:process'
import { loadEnv } from 'vite'
import type { ConfigEnv, UserConfig } from 'vite'
import { createVitePlugins } from './build/vite'
import { exclude, include } from './build/vite/optimize'

export default ({ mode }: ConfigEnv): UserConfig => {
  const root = process.cwd()
  const env = loadEnv(mode, root)

  return {
    base: env.VITE_APP_PUBLIC_PATH,
    plugins: createVitePlugins(mode),

    server: {
      host: true,
      port: 3000,
      allowedHosts: ['760329wrxu23.vicp.fun'],
      // ponytail: 允许 import 仓库根 docs/
      fs: {
        allow: [path.join(__dirname, '../..')],
      },
      proxy: {
        '/api': {
          target: 'http://localhost:8000',
          ws: false,
          changeOrigin: true,
          rewrite: path => path.replace(/^\/api/, ''),
        },
        '/uploads': {
          target: 'http://localhost:8000',
          ws: false,
          changeOrigin: true,
        },
      },
    },

    resolve: {
      alias: {
        '@': path.join(__dirname, './src'),
        '~': path.join(__dirname, './src/assets'),
        '~root': path.join(__dirname, '.'),
        // ponytail: 设置页读仓库 docs/USER_CHANGELOG.md，避免前端再抄一份
        '~docs': path.join(__dirname, '../../docs'),
      },
    },

    build: {
      cssCodeSplit: false,
      chunkSizeWarningLimit: 2048,
      outDir: env.VITE_APP_OUT_DIR || 'dist',
    },

    optimizeDeps: { include, exclude },
  }
}
