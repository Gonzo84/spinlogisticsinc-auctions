import { themeConfig, globalPT } from '@auction-platform/design-tokens'

export default defineNuxtConfig({
  ssr: true,

  modules: [
    '@pinia/nuxt',
    '@vueuse/nuxt',
    '@nuxtjs/tailwindcss',
    '@nuxtjs/i18n',
    '@nuxt/eslint',
    '@primevue/nuxt-module',
  ],

  primevue: {
    options: { theme: themeConfig, pt: globalPT },
    autoImport: true,
  },

  css: ['~/assets/css/main.css'],

  i18n: {
    locales: [
      { code: 'sl', name: 'Slovenščina', file: 'sl.json' },
      { code: 'hr', name: 'Hrvatski', file: 'hr.json' },
      { code: 'de', name: 'Deutsch', file: 'de.json' },
      { code: 'en', name: 'English', file: 'en.json' },
      { code: 'it', name: 'Italiano', file: 'it.json' },
      { code: 'sr', name: 'Srpski', file: 'sr.json' },
      { code: 'hu', name: 'Magyar', file: 'hu.json' },
    ],
    defaultLocale: 'en',
    langDir: 'locales',
    // @ts-expect-error lazy is valid but not in generated types for this i18n version
    lazy: true,
    strategy: 'prefix_except_default',
    detectBrowserLanguage: {
      useCookie: true,
      cookieKey: 'i18n_redirected',
      redirectOn: 'root',
    },
  },

  tailwindcss: {
    configPath: 'tailwind.config.ts',
    // Prevent the module from injecting a second (unlayered) copy of Tailwind.
    // main.css already wraps @tailwind directives inside @layer tailwind-base /
    // tailwind-utilities so that PrimeVue's @layer primevue is not overridden
    // by Preflight's universal reset (border:0, padding:0).
    cssPath: false,
  },

  runtimeConfig: {
    public: {
      apiBaseUrl: process.env.NUXT_PUBLIC_API_BASE_URL || 'http://localhost:8080/api/v1',
      wsBaseUrl: process.env.NUXT_PUBLIC_WS_BASE_URL || 'ws://localhost:8080/ws',
      keycloakUrl: process.env.NUXT_PUBLIC_KEYCLOAK_URL || 'http://localhost:8180',
      keycloakRealm: process.env.NUXT_PUBLIC_KEYCLOAK_REALM || 'auction-platform',
      keycloakClientId: process.env.NUXT_PUBLIC_KEYCLOAK_CLIENT_ID || 'buyer-web',
      sellerPortalUrl: process.env.NUXT_PUBLIC_SELLER_PORTAL_URL || 'http://localhost:5174',
      adminDashboardUrl: process.env.NUXT_PUBLIC_ADMIN_DASHBOARD_URL || 'http://localhost:5175',
      brokerPortalUrl: process.env.NUXT_PUBLIC_BROKER_PORTAL_URL || 'http://localhost:3003',
    },
  },

  app: {
    head: {
      title: 'Spin Logistics - Your Trusted Logistics Partner',
      charset: 'utf-8',
      viewport: 'width=device-width, initial-scale=1',
      meta: [
        { name: 'description', content: 'Spin Logistics Inc. — Interstate freight carrier. General freight, metal sheet, building materials hauling across the US. Based in Chicago, IL.' },
        { name: 'format-detection', content: 'telephone=no' },
        { property: 'og:site_name', content: 'Spin Logistics' },
        { property: 'og:locale', content: 'en_US' },
      ],
      link: [
        { rel: 'preconnect', href: 'https://fonts.googleapis.com' },
        { rel: 'preconnect', href: 'https://fonts.gstatic.com', crossorigin: '' },
        { rel: 'stylesheet', href: 'https://fonts.googleapis.com/css2?family=Barlow+Condensed:wght@500;600;700&family=Open+Sans:wght@400;500;600;700&display=swap' },
      ],
      htmlAttrs: {
        lang: 'en',
      },
    },
  },

  routeRules: {
    '/': { prerender: true },
    '/search': { swr: 300 },
    '/lots/**': { ssr: false },
    '/my/**': { ssr: false },
    '/checkout/**': { ssr: false },
    '/auth/**': { ssr: false },
    '/profile/**': { ssr: false },
    '/watchlist': { redirect: '/my/watchlist' },
    '/account/watchlist': { redirect: '/my/watchlist' },
  },

  nitro: {
    compressPublicAssets: true,
    routeRules: {
      '/**': {
        headers: {
          'X-Content-Type-Options': 'nosniff',
          'X-Frame-Options': 'DENY',
          'X-XSS-Protection': '1; mode=block',
          'Referrer-Policy': 'strict-origin-when-cross-origin',
          'Permissions-Policy': 'camera=(), microphone=(), geolocation=(self)',
          'Strict-Transport-Security': 'max-age=31536000; includeSubDomains',
        },
      },
      '/silent-check-sso.html': {
        headers: {
          'X-Frame-Options': 'SAMEORIGIN',
        },
      },
    },
  },

  devtools: { enabled: true },

  compatibilityDate: '2024-11-01',
})
