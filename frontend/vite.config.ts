import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { VitePWA } from 'vite-plugin-pwa';

export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: 'autoUpdate',
      manifest: {
        name: 'Poultry Production Management System',
        short_name: 'PPMS',
        description: 'Management platform for integrated poultry production',
        theme_color: '#1B5E20',
        background_color: '#ffffff',
        display: 'standalone',
        start_url: '/',
      },
      workbox: {
        navigateFallback: '/index.html',
        runtimeCaching: [{
          urlPattern: /^https?:\/\/.*\/api\//,
          handler: 'NetworkFirst',
          options: { cacheName: 'ppms-api', networkTimeoutSeconds: 5 },
        }],
      },
    }),
  ],
});
