import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

const apiTarget = process.env.VITE_API_TARGET || 'http://localhost:8081';

export default defineConfig({
  plugins: [react()],
  build: {
    chunkSizeWarningLimit: 750
  },
  server: {
    port: 5173,
    proxy: {
      '/api': apiTarget
    }
  }
});
