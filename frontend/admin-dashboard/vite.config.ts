import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { PrimeVueResolver } from '@primevue/auto-import-resolver'
import { resolve } from 'path'

export default defineConfig({
  plugins: [
    vue(),
    Components({
      resolvers: [PrimeVueResolver()],
      dts: true,
    }),
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 5175,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    target: 'esnext',
    sourcemap: true,
    rollupOptions: {
      output: {
        manualChunks: {
          'primevue-core': ['primevue/config', 'primevue/button', 'primevue/datatable', 'primevue/column'],
          'primevue-form': ['primevue/inputtext', 'primevue/select', 'primevue/textarea', 'primevue/datepicker', 'primevue/inputnumber'],
          'primevue-overlay': ['primevue/dialog', 'primevue/popover', 'primevue/tooltip', 'primevue/drawer'],
        },
      },
    },
  },
})
