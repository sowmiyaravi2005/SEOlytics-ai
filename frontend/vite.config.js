import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

const apiTarget = process.env.VITE_API_TARGET || process.env.VITE_API_URL || 'http://localhost:8081';

export default defineConfig({
  plugins: [react()],
  build: {
    chunkSizeWarningLimit: 750
  },
  preview: {
    host: '0.0.0.0',
    port: Number(process.env.PORT) || 4173,
    allowedHosts: ['seolytics-ai.onrender.com']
  },
  server: {
    port: 5173,
    proxy: {
      '/api': apiTarget
    }
  }
});
